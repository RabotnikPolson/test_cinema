package com.cinema.testcinema.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * REST-клиент для TMDB API (TheMovieDB).
 * Используется для обогащения данных: поиск tmdb_id фильма по названию и году.
 * Docs: https://developer.themoviedb.org/reference/search-movie
 */
@Component
public class TmdbClient {

    private static final Logger log = LoggerFactory.getLogger(TmdbClient.class);
    private static final String TMDB_SEARCH_URL = "https://api.themoviedb.org/3/search/movie";

    private final RestClient restClient;

    @Value("${tmdb.api.token}")
    private String apiToken;

    public TmdbClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * Ищет фильм в TMDB по названию и году.
     *
     * @param title Название фильма (на любом языке)
     * @param year  Год выпуска (может быть null)
     * @return TMDB ID фильма или null если не найден
     */
    public Long searchMovieId(String title, Integer year) {
        if (title == null || title.isBlank()) {
            return null;
        }

        try {
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
            StringBuilder uri = new StringBuilder(TMDB_SEARCH_URL)
                    .append("?query=").append(encodedTitle)
                    .append("&language=ru-RU");

            if (year != null && year > 0) {
                uri.append("&year=").append(year);
            }

            log.info("[TMDB] Searching movie: title='{}', year={}", title, year);

            JsonNode response = restClient.get()
                    .uri(uri.toString())
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiToken)
                    .retrieve()
                    .body(JsonNode.class);

            if (response != null && response.has("results")
                    && response.get("results").isArray()
                    && !response.get("results").isEmpty()) {
                Long tmdbId = response.get("results").get(0).get("id").asLong();
                log.info("[TMDB] Found TMDB ID: {} for '{}'", tmdbId, title);
                return tmdbId;
            }

            log.info("[TMDB] No results found for '{}'", title);
            return null;

        } catch (Exception e) {
            log.error("[TMDB] Error searching for title='{}': {}", title, e.getMessage());
            return null;
        }
    }
}
