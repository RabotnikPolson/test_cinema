package com.cinema.testcinema.service;

import com.cinema.testcinema.client.OpenSubtitlesClient;
import com.cinema.testcinema.exception.OpenSubtitlesQuotaExceededException;
import com.cinema.testcinema.model.MovieSubtitle;
import com.cinema.testcinema.repository.MovieSubtitleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SubtitleDownloadWorker {

    private static final Logger log = LoggerFactory.getLogger(SubtitleDownloadWorker.class);

    private final MovieSubtitleRepository subtitleRepository;
    private final OpenSubtitlesClient openSubtitlesClient;
    private final com.cinema.testcinema.client.SubtitleTranslationClient translationClient;

    @Value("${storage.local.base-path:./storage}")
    private String basePath;

    private final AtomicBoolean isQuotaExhausted = new AtomicBoolean(false);
    private Instant resumeAt = Instant.MIN;

    public SubtitleDownloadWorker(MovieSubtitleRepository subtitleRepository, 
                                  OpenSubtitlesClient openSubtitlesClient,
                                  com.cinema.testcinema.client.SubtitleTranslationClient translationClient) {
        this.subtitleRepository = subtitleRepository;
        this.openSubtitlesClient = openSubtitlesClient;
        this.translationClient = translationClient;
    }

    @Scheduled(fixedDelay = 1800000)
    public void processDownloadQueue() {
        if (isQuotaExhausted.get()) {
            if (Instant.now().isBefore(resumeAt)) {
                log.info("Worker sleeping. OpenSubtitles hard limit of 20/day is active. Resumes at {}", resumeAt);
                return;
            } else {
                log.info("Waking up worker. Resetting quota lock.");
                isQuotaExhausted.set(false);
            }
        }

        Optional<MovieSubtitle> pendingOpt = subtitleRepository.findFirstByIsDownloadedFalseOrderByCreatedAtAsc();
        if (pendingOpt.isEmpty()) {
            return;
        }

        MovieSubtitle subtitle = pendingOpt.get();
        log.info("Worker selected subtitle ID {} (osFileId: {}) for movie '{}'", 
                subtitle.getId(), subtitle.getOsFileId(), subtitle.getMovie().getTitle());

        try {
            String link = openSubtitlesClient.requestDownloadLink(subtitle.getOsFileId());
            if (link != null) {
                byte[] fileBytes = openSubtitlesClient.downloadFileActual(link);
                if (fileBytes != null) {
                    saveFileToDisk(subtitle, fileBytes);
                    subtitle.setDownloaded(true);
                    subtitle.setTranslationStatus("in_progress");
                    subtitleRepository.save(subtitle);
                    log.info("Successfully downloaded and saved subtitle to {}", subtitle.getLocalPath());
                    
                    // Преобразуем относительный путь в абсолютный, чтобы избежать "конфликта рабочих директорий"
                    // между Java (запущена в /testCinema) и Python (запущен в /subtitle-translator)
                    String absoluteInputPath = java.nio.file.Path.of(subtitle.getLocalPath()).toAbsolutePath().normalize().toString().replace('\\', '/');

                    String absoluteOutputPath = absoluteInputPath
                            .replace(subtitle.getLanguage() + ".vtt", "kk.srt")
                            .replace(subtitle.getLanguage() + ".srt", "kk.srt");

                    translationClient.triggerTranslation(
                            subtitle.getMovie().getId(),
                            absoluteInputPath,
                            absoluteOutputPath,
                            subtitle.getMovie().getTitle(),
                            subtitle.getLanguage()
                    );
                } else {
                    log.error("Failed to download bytes from OS link for fileId {}", subtitle.getOsFileId());
                }
            } else {
                 log.error("Received null link from OS for fileId {}", subtitle.getOsFileId());
            }
        } catch (OpenSubtitlesQuotaExceededException e) {
            log.warn("=== OPENSUBTITLES QUOTA LIMIT REACHED (20 downloads/day) ===");
            log.warn("Exception: {}", e.getMessage());
            isQuotaExhausted.set(true);
            resumeAt = Instant.now().plus(24, ChronoUnit.HOURS);
            log.warn("Worker paused until {}", resumeAt);
        } catch (Exception e) {
            log.error("Unexpected error during subtitle download queue: {}", e.getMessage());
        }
    }

    private void saveFileToDisk(MovieSubtitle subtitle, byte[] bytes) throws IOException {
        Path dirPath = Path.of(basePath, "subtitles", String.valueOf(subtitle.getMovie().getId()));
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        
        Path filePath = dirPath.resolve(subtitle.getLanguage() + ".vtt");
        Files.write(filePath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        
        subtitle.setLocalPath(filePath.toString().replace('\\', '/'));
    }
}
