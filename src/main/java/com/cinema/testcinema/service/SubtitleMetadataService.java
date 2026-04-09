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

    private final OpenSubtitlesClient openSubtitlesClient;
    private final MovieSubtitleRepository subtitleRepository;

    public SubtitleMetadataService(OpenSubtitlesClient openSubtitlesClient, MovieSubtitleRepository subtitleRepository) {
        this.openSubtitlesClient = openSubtitlesClient;
        this.subtitleRepository = subtitleRepository;
    }

    /**
     * Finds subtitle metadata via API but does not download the file!
     * Protects the 20 downloads/day limit.
     */
    @Transactional
    public void discoverForMovie(Movie movie) {
        if (movie.getImdbId() == null || movie.getImdbId().isEmpty()) {
            log.debug("Movie {} has no IMDB ID to search subtitles.", movie.getId());
            return;
        }

        String targetLang = determineLanguageByCountry(movie.getCountry());

        if (subtitleRepository.existsByMovieIdAndLanguage(movie.getId(), targetLang)) {
            log.debug("Subtitle info for movie {} with lang {} already exists.", movie.getId(), targetLang);
            return;
        }

        String fileId = openSubtitlesClient.searchSubtitles(movie.getImdbId(), targetLang);
        if (fileId != null) {
            MovieSubtitle subtitle = new MovieSubtitle(movie, targetLang, fileId);
            subtitle.setDownloaded(false);
            subtitleRepository.save(subtitle);
            log.info("Found {} subtitle for movie '{}' (ID: {}). Enqueued for download.", targetLang, movie.getTitle(), movie.getId());
        } else {
            log.warn("No {} subtitles found for movie '{}' (ID: {})", targetLang, movie.getTitle(), movie.getId());
        }
    }

    private String determineLanguageByCountry(String countryStr) {
        if (countryStr == null) return "en";
        String country = countryStr.toLowerCase();
        List<String> cisKeywords = List.of("росс", "ссср", "казах", "украин", "беларус", "киргиз", "узбек");

        boolean isCis = cisKeywords.stream().anyMatch(country::contains);
        return isCis ? "ru" : "en";
    }
}
