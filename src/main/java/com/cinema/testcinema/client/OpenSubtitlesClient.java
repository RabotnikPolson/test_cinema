package com.cinema.testcinema.client;

import com.cinema.testcinema.exception.OpenSubtitlesQuotaExceededException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

@Component
public class OpenSubtitlesClient {

    private static final Logger log = LoggerFactory.getLogger(OpenSubtitlesClient.class);

    private final RestClient restClient;

    @Value("${opensubtitles.api.url:https://api.opensubtitles.com/api/v1}")
    private String apiUrl;

    // ВНИМАНИЕ: Проверьте, чтобы в properties было opensubtitles.api.key
    @Value("${opensubtitles.api.key}")
    private String apiKey;

    @Value("${opensubtitles.username}")
    private String username;

    @Value("${opensubtitles.password}")
    private String password;

    private volatile String currentToken;
    private Instant tokenExpiresAt = Instant.MIN;

    public OpenSubtitlesClient(RestClient.Builder restClientBuilder) {
        // Добавляем обязательный User-Agent
        this.restClient = restClientBuilder
                .defaultHeader("User-Agent", "TestCinemaApp v1.0")
                .build();
    }

    private synchronized void refreshTokenIfNeeded() {
        if (currentToken == null || Instant.now().isAfter(tokenExpiresAt)) {
            log.info("Refreshing OpenSubtitles JWT token for user: {}", username);
            try {
                JsonNode response = restClient.post()
                        .uri(apiUrl + "/login")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Api-Key", apiKey)
                        .body(Map.of("username", username, "password", password))
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (req, res) -> {
                            log.error("Login failed with status: {}", res.getStatusCode());
                            handleRateLimits(res.getStatusCode().value());
                        })
                        .body(JsonNode.class);

                if (response != null && response.has("token")) {
                    this.currentToken = response.get("token").asText();
                    // Токен обычно живет 24 часа, ставим запас
                    this.tokenExpiresAt = Instant.now().plusSeconds(80000);
                    log.info("OpenSubtitles token successfully refreshed.");
                } else {
                    throw new RuntimeException("Login response does not contain a token");
                }
            } catch (Exception e) {
                log.error("Failed to refresh OpenSubtitles token: {}", e.getMessage());
                throw new RuntimeException("Could not login to OpenSubtitles API", e);
            }
        }
    }

    public String searchSubtitles(String imdbId, String language) {
        refreshTokenIfNeeded();
        try {
            Long numericImdbId = parseImdbId(imdbId);
            log.info("Searching subtitles for IMDB: {}, Lang: {}", numericImdbId, language);

            JsonNode response = restClient.get()
                    .uri(apiUrl + "/subtitles?imdb_id=" + numericImdbId + "&languages=" + language)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Api-Key", apiKey)
                    .header("Authorization", "Bearer " + currentToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.error("Search failed: {}", res.getStatusCode());
                        handleRateLimits(res.getStatusCode().value());
                    })
                    .body(JsonNode.class);

            if (response != null && response.has("data") && response.get("data").isArray() && !response.get("data").isEmpty()) {
                JsonNode firstElement = response.get("data").get(0);
                if (firstElement.has("attributes") && firstElement.get("attributes").has("files")) {
                    JsonNode files = firstElement.get("attributes").get("files");
                    if (files.isArray() && !files.isEmpty()) {
                        return files.get(0).get("file_id").asText();
                    }
                }
            }
            return null;
        } catch (OpenSubtitlesQuotaExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error searching subtitles for IMDb {} : {}", imdbId, e.getMessage());
            return null;
        }
    }

    public String requestDownloadLink(String osFileId) {
        refreshTokenIfNeeded();
        try {
            JsonNode response = restClient.post()
                    .uri(apiUrl + "/download")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Api-Key", apiKey)
                    .header("Authorization", "Bearer " + currentToken)
                    .body(Map.of("file_id", Long.parseLong(osFileId)))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> handleRateLimits(res.getStatusCode().value()))
                    .body(JsonNode.class);

            if (response != null && response.has("link")) {
                return response.get("link").asText();
            }
        } catch (OpenSubtitlesQuotaExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to request download link for fileId {}: {}", osFileId, e.getMessage());
        }
        return null;
    }

    public byte[] downloadFileActual(String link) {
        try {
            return restClient.get()
                    .uri(link)
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception e) {
            log.error("Failed to download actual subtitle bytes from link {}: {}", link, e.getMessage());
            return null;
        }
    }

    private void handleRateLimits(int statusCode) {
        if (statusCode == 406 || statusCode == 429) {
            throw new OpenSubtitlesQuotaExceededException("OpenSubtitles quota exhausted! Status code: " + statusCode);
        }
        if (statusCode == 401 || statusCode == 403) {
            throw new RuntimeException("Authentication failed (401/403). Check API Key and Credentials.");
        }
        if (statusCode >= 400 && statusCode < 500) {
            throw new RuntimeException("Client Error: " + statusCode);
        }
        if (statusCode >= 500) {
            throw new RuntimeException("Server Error: " + statusCode);
        }
    }

    private Long parseImdbId(String imdbId) {
        if (imdbId != null && imdbId.startsWith("tt")) {
            return Long.parseLong(imdbId.substring(2));
        }
        return Long.parseLong(imdbId);
    }
}