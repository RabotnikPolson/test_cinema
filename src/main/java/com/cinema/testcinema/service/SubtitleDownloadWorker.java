package com.cinema.testcinema.service;

import com.cinema.testcinema.client.OpenSubtitlesClient;
import com.cinema.testcinema.dto.subtitle.DownloadLinkDto;
import com.cinema.testcinema.model.MovieSubtitle;
import com.cinema.testcinema.repository.SubtitleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubtitleDownloadWorker {

    private static final String ERROR_SENTINEL = "ERROR_DOWNLOAD_FAILED";

    private final SubtitleRepository subtitleRepository;
    private final OpenSubtitlesClient openSubtitlesClient;
    private final StorageService storageService;

    // RestClient с жёсткими таймаутами для прямого скачивания файла
    private final RestClient fileDownloadClient = buildFileDownloadClient();

    // Запуск каждые 5 минут
    @Scheduled(fixedDelay = 30_000)
    public void processDownloadQueue() {
        List<MovieSubtitle> pending = subtitleRepository.findPendingDownloads();

        if (pending.isEmpty()) {
            log.info("[Worker] No pending subtitles to download.");
            return;
        }

        log.info("[Worker] Found {} subtitle(s) pending download.", pending.size());

        for (MovieSubtitle sub : pending) {
            boolean shouldStop = processSingleSubtitle(sub);
            if (shouldStop) {
                log.warn("[Worker] Stopping queue processing early (daily limit reached or critical error).");
                break;
            }
        }
    }

    /**
     * @return true — нужно останавливать обработку очереди; false — продолжаем.
     */
    private boolean processSingleSubtitle(MovieSubtitle sub) {
        String savedPath = null;

        try {
            log.info("[Worker] Processing subtitle id={} osFileId={} movie={}", sub.getId(), sub.getOsFileId(), sub.getMovie().getId());

            // ── Шаг 1: Получаем ссылку на скачивание (тратим квоту) ─────────────────
            DownloadLinkDto linkDto;
            try {
                linkDto = openSubtitlesClient.requestDownloadLink(sub.getOsFileId());
            } catch (HttpClientErrorException e) {
                int status = e.getStatusCode().value();
                if (status == 406 || status == 429) {
                    log.warn("[Worker] DAILY LIMIT REACHED (HTTP {}). Stopping queue.", status);
                    return true; // Стоп
                }
                log.error("[Worker] API error {} for subtitle id={}. Marking as failed.", status, sub.getId());
                markAsFailed(sub);
                return false; // Продолжаем следующий
            }

            // ── Шаг 2: Проверяем остаток квоты из ответа API ────────────────────────
            int remaining = linkDto.remainingRequests() != null ? linkDto.remainingRequests() : 0;
            if (remaining <= 0) {
                log.warn("[Worker] DAILY LIMIT REACHED (remainingRequests={}). Stopping queue.", remaining);
                return true; // Стоп
            }

            if (linkDto.link() == null || linkDto.link().isBlank()) {
                log.warn("[Worker] API returned empty link for subtitle id={}. Marking as failed.", sub.getId());
                markAsFailed(sub);
                return false;
            }

            // ── Шаг 3: Скачиваем файл с таймаутом через RestClient ───────────────────
            String extension = "." + (sub.getFormat() != null ? sub.getFormat() : "srt");
            byte[] fileBytes = fileDownloadClient.get()
                    .uri(linkDto.link())
                    .retrieve()
                    .body(byte[].class);

            if (fileBytes == null || fileBytes.length == 0) {
                log.warn("[Worker] Downloaded empty file for subtitle id={}. Marking as failed.", sub.getId());
                markAsFailed(sub);
                return false;
            }

            // ── Шаг 4: Сохраняем файл на диск ───────────────────────────────────────
            try (InputStream in = new java.io.ByteArrayInputStream(fileBytes)) {
                savedPath = storageService.storeFile(sub.getMovie().getId(), sub.getOsFileId(), in, extension);
            }

            // ── Шаг 5: Обновляем запись в БД (компенсирующая транзакция) ────────────
            try {
                sub.setDownloaded(true);
                sub.setStoragePath(savedPath);
                subtitleRepository.save(sub);
                log.info("[Worker] Subtitle id={} saved at '{}'. Remaining quota: {}", sub.getId(), savedPath, remaining);
            } catch (Exception dbEx) {
                // БД упала — откатываем физический файл ("файл-призрак" зачищаем)
                log.error("[Worker] DB save failed for subtitle id={}. Rolling back file: {}", sub.getId(), savedPath, dbEx);
                storageService.deleteFile(savedPath);
                savedPath = null;
                markAsFailed(sub);
            }

        } catch (HttpServerErrorException e) {
            // ЛОВИМ 5xx ОШИБКИ (Временное падение сервера OpenSubtitles)
            log.warn("[Worker] OpenSubtitles server is temporarily down ({}). Will retry next cycle. Subtitle id={}",
                    e.getStatusCode(), sub.getId());
            if (savedPath != null) storageService.deleteFile(savedPath);
            return false; // НЕ вызываем markAsFailed! Оставляем в очереди.

        } catch (ResourceAccessException e) {
            // ЛОВИМ ТАЙМАУТЫ СЕТИ (Сервер не ответил за 15 секунд)
            log.warn("[Worker] Network timeout/connection issue. Will retry next cycle. Subtitle id={}", sub.getId());
            if (savedPath != null) storageService.deleteFile(savedPath);
            return false; // НЕ вызываем markAsFailed! Оставляем в очереди.

        } catch (RestClientException e) {
            // Остальные (постоянные) ошибки сети
            log.error("[Worker] Network error downloading subtitle id={}. Marking as failed.", sub.getId(), e);
            if (savedPath != null) storageService.deleteFile(savedPath);
            markAsFailed(sub);

        } catch (Exception e) {
            // Непредвиденные ошибки кода/ФС
            log.error("[Worker] Unexpected error for subtitle id={}. Marking as failed.", sub.getId(), e);
            if (savedPath != null) storageService.deleteFile(savedPath);
            markAsFailed(sub);
        }

        return false; // Продолжаем очередь
    }

    /**
     * «Poison Pill» защита: маркируем файл как обработанный с признаком ошибки.
     * Воркер больше НЕ будет его трогать (is_downloaded = true).
     */
    private void markAsFailed(MovieSubtitle sub) {
        try {
            sub.setDownloaded(true);
            sub.setStoragePath(ERROR_SENTINEL);
            sub.setNeedsTranslation(false);
            subtitleRepository.save(sub);
            log.info("[Worker] Subtitle id={} marked as FAILED and removed from queue.", sub.getId());
        } catch (Exception e) {
            log.error("[Worker] CRITICAL: cannot mark subtitle id={} as failed. It will be retried next cycle!", sub.getId(), e);
        }
    }

    /**
     * RestClient с жёсткими таймаутами для скачивания файлов субтитров напрямую по URL.
     * Connect: 5s, Read: 15s
     */
    private static RestClient buildFileDownloadClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);  // 5 секунд на установку соединения
        factory.setReadTimeout(15_000);    // 15 секунд на чтение файла
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}