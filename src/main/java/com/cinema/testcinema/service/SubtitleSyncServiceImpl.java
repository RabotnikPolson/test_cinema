package com.cinema.testcinema.service;

import com.cinema.testcinema.client.OpenSubtitlesClient;
import com.cinema.testcinema.dto.subtitle.SubtitleInfoDto;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.MovieSubtitle;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.SubtitleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubtitleSyncServiceImpl implements SubtitleSyncService {

    private final OpenSubtitlesClient openSubtitlesClient;
    private final SubtitleRepository subtitleRepository;
    private final MovieRepository movieRepository;

    @Override
    @Transactional
    public void discoverAndStageSubtitles(Long movieId, String imdbId) {
        log.info("Discovering subtitles for movie {} (IMDB: {})", movieId, imdbId);
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new IllegalArgumentException("Movie not found"));

        // Ищем английские и русские субтитры (Сырье для перевода)
        List<SubtitleInfoDto> subtitles = openSubtitlesClient.searchSubtitles(imdbId, List.of("en", "ru"));
        
        if (subtitles == null || subtitles.isEmpty()) {
            log.warn("No suitable subtitles found for movie {}", movieId);
            return;
        }

        // Берем лучший (самый высокий рейтинг или первый в списке)
        SubtitleInfoDto bestSub = subtitles.get(0); // Предполагая, что API возвращает отсортированный по рейтингу список
        
        // Проверяем, есть ли уже такой файл в БД
        Optional<MovieSubtitle> existing = subtitleRepository.findByOsFileId(bestSub.osFileId());
        if (existing.isEmpty()) {
            MovieSubtitle sub = MovieSubtitle.builder()
                .movie(movie)
                .originalLanguage(bestSub.language())
                .osFileId(bestSub.osFileId())
                .osSubtitleId(bestSub.osSubtitleId())
                .format(bestSub.format())
                .isDownloaded(false)
                .needsTranslation(true)
                .targetLanguage("kk")
                .isVectorizedForAi(false)
                .build();
            subtitleRepository.save(sub);
            log.info("Staged subtitle {} for download & translation", bestSub.osFileId());
        } else {
            log.info("Subtitle {} is already staged", bestSub.osFileId());
        }
    }

    @Override
    public void processStagedSubtitleDownload(Long subtitleId) {
        // Логика скачивания будет вызываться воркером.
        // Эту часть мы переносим напрямую в логику SubtitleDownloadWorker или оставляем здесь как утилиту.
    }
}
