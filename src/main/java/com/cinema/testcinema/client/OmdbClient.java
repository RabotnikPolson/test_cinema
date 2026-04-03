package com.cinema.testcinema.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Fetches IMDB IDs from OMDb API for movies that lack imdbId after Kinopoisk sync.
 * Used as Level 2 in the subtitle search cascade.
 * OMDb docs: https://www.omdbapi.com/
 */
@Component
public class OmdbClient {

    private static final Logger log = LoggerFactory.getLogger(OmdbClient.class);

    private final RestClient restClient;

    @Value("${omdb.api.url}")
    private String apiUrl;

    @Value("${omdb.api.key}")
    private String apiKey;

    public OmdbClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    /**
     * Looks up a movie by title and optional year.
     * Uses OMDb "By Title" mode (?t=...) which returns a single best match.
     *
     * @param title Movie title to search for
     * @param year  Optional release year (improves accuracy, can be null)
     * @return IMDB ID (e.g. "tt7985648") or null if not found
     */
    public String findImdbId(String title, Long year) {
        log.info("[OMDb] Looking up IMDB ID for title='{}' year={}", title, year);
        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                    .fromHttpUrl(apiUrl)
                    .queryParam("apikey", apiKey)
                    .queryParam("t", title)
                    .queryParam("type", "movie");

            if (year != null) {
                uriBuilder.queryParam("y", year);
            }

            String uri = uriBuilder.toUriString();
            JsonNode response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                log.warn("[OMDb] Empty response for title='{}'", title);
                return null;
            }

            // OMDb returns {"Response":"False","Error":"Movie not found!"} on miss
            if ("False".equals(response.path("Response").asText())) {
                log.info("[OMDb] Not found: {}", response.path("Error").asText("unknown error"));
                return null;
            }

            String imdbId = response.path("imdbID").asText(null);
            if (imdbId != null && !imdbId.isBlank()) {
                log.info("[OMDb] Found IMDB ID: {} for title='{}'", imdbId, title);
                return imdbId;
            }

            return null;

        } catch (Exception e) {
            log.error("[OMDb] Error looking up title='{}'", title, e);
            return null;
        }
    }
}
