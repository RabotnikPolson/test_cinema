package com.cinema.testcinema.service;

import com.cinema.testcinema.client.OpenSubtitlesClient;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.MovieSubtitle;
import com.cinema.testcinema.repository.MovieSubtitleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubtitleMetadataService {

    private static final Logger log = LoggerFactory.getLogger(SubtitleMetadataService.class);

    private static final List<String> CIS_KEYWORDS = List.of(
            "росс", "ссср", "казах", "украин", "беларус", "киргиз", "узбек",
            "таджик", "туркмен", "молдав", "грузи", "армен", "азербайджан", "латви", "литв", "эстон"
    );

    private final OpenSubtitlesClient openSubtitlesClient;
    private final MovieSubtitleRepository subtitleRepository;

    public SubtitleMetadataService(OpenSubtitlesClient openSubtitlesClient,
                                   MovieSubtitleRepository subtitleRepository) {
        this.openSubtitlesClient = openSubtitlesClient;
        this.subtitleRepository = subtitleRepository;
    }

    @Transactional
    public void discoverForMovie(Movie movie) {
        Long movieId = movie.getId();
        String title = movie.getTitle();
        String imdbId = movie.getImdbId();
        Long tmdbId = movie.getTmdbId();

        log.info("╔══════════════════════════════════════════════════════════════");
        log.info("║ Каскадный поиск субтитров для: '{}' (ID: {}, IMDB: {}, TMDB: {})", title, movieId, imdbId, tmdbId);
        log.info("║ Страна: {}", movie.getCountry());
        log.info("╚══════════════════════════════════════════════════════════════");

        log.info("[Cascade Step 1/3] Ищем казахские субтитры (kk)...");
        if (tryDiscoverAndSave(movie, imdbId, tmdbId, "kk")) {
            return;
        }
        log.info("[Cascade Step 1/3] Казахские субтитры не найдены.");

        boolean isCis = isCisCountry(movie.getCountry());
        if (isCis) {
            log.info("[Cascade Step 2/3] Страна '{}' определена как СНГ. Ищем русские субтитры (ru)...",
                    movie.getCountry());
            if (tryDiscoverAndSave(movie, imdbId, tmdbId, "ru")) {
                return;
            }
            log.info("[Cascade Step 2/3] Русские субтитры не найдены.");
        } else {
            log.info("[Cascade Step 2/3] Страна '{}' — не СНГ. Пропускаем поиск ru.", movie.getCountry());
        }

        log.info("[Cascade Step 3/3] Ищем английские субтитры (en)...");
        if (tryDiscoverAndSave(movie, imdbId, tmdbId, "en")) {
            return;
        }
        log.info("[Cascade Step 3/3] Английские субтитры не найдены.");

        log.warn("⚠ No suitable base subtitles (kk, ru, en) found for movie '{}' (ID: {}). " +
                "Subtitle queue is empty for this movie.", title, movieId);
    }

    private boolean tryDiscoverAndSave(Movie movie, String imdbId, Long tmdbId, String lang) {
        if (subtitleRepository.existsByMovieIdAndLanguage(movie.getId(), lang)) {
            log.info("  → Субтитры ({}) для этого фильма уже есть в БД. Пропускаем.", lang);
            return true; // Уже есть — считаем успехом
        }

        String fileId = openSubtitlesClient.searchSubtitles(imdbId, tmdbId, lang);
        if (fileId != null) {
            MovieSubtitle subtitle = new MovieSubtitle(movie, lang, fileId);
            subtitle.setDownloaded(false);
            subtitleRepository.save(subtitle);
            log.info("  ✓ Найдены {} субтитры (osFileId: {}). Поставлены в очередь на скачивание.", lang, fileId);
            return true;
        }
        return false;
    }

    private boolean isCisCountry(String countryStr) {
        if (countryStr == null || countryStr.isBlank()) return false;
        String lower = countryStr.toLowerCase();
        return CIS_KEYWORDS.stream().anyMatch(lower::contains);
    }
}
