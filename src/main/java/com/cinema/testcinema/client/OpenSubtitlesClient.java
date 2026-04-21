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

    @Value("${opensubtitles.api.key}")
    private String apiKey;

    @Value("${opensubtitles.username}")
    private String username;

    @Value("${opensubtitles.password}")
    private String password;

    private volatile String currentToken;
    private Instant tokenExpiresAt = Instant.MIN;

    public OpenSubtitlesClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .defaultHeader("User-Agent", "TestCinemaApp v1.0")
                .build();
    }

    // ── Аутентификация ────────────────────────────────────────────────────

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
                    this.tokenExpiresAt = Instant.now().plusSeconds(80000); // ~22 часа
                    log.info("OpenSubtitles token successfully refreshed.");
                } else {
                    throw new RuntimeException("Login response does not contain a token");
                }
            } catch (OpenSubtitlesQuotaExceededException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to refresh OpenSubtitles token: {}", e.getMessage());
                throw new RuntimeException("Could not login to OpenSubtitles API", e);
            }
        }
    }

    // ── Поиск субтитров (по imdbId ИЛИ по tmdbId) ────────────────────────

    /**
     * Ищет субтитры на OpenSubtitles.
     * <p>
     * Стратегия:
     * 1. Если imdbId не пустой → ищем по imdb_id (самый точный).
     * 2. Если imdbId пустой, но tmdbId != null → ищем по tmdb_id (фоллбэк).
     * 3. Если оба пусты → возвращаем null (нечего искать).
     *
     * @param imdbId IMDB ID фильма (может быть null/пустым)
     * @param tmdbId TMDB ID фильма (может быть null)
     * @param lang   Код языка субтитров ('kk', 'ru', 'en')
     * @return os_file_id найденного файла субтитров, или null если ничего нет
     */
    public String searchSubtitles(String imdbId, Long tmdbId, String lang) {
        // Определяем параметр поиска
        String searchParam;
        if (imdbId != null && !imdbId.isBlank()) {
            searchParam = "imdb_id=" + parseImdbId(imdbId);
        } else if (tmdbId != null) {
            searchParam = "tmdb_id=" + tmdbId;
        } else {
            log.debug("Movie has no IMDB ID and no TMDB ID — skipping OpenSubtitles search.");
            return null;
        }

        refreshTokenIfNeeded();
        try {
            String uri = apiUrl + "/subtitles?" + searchParam + "&languages=" + lang + "&foreign_parts_only=exclude&order_by=download_count&order_direction=desc";            log.info("OS Search request: {}", uri);

            JsonNode response = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Api-Key", apiKey)
                    .header("Authorization", "Bearer " + currentToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.error("Search failed: {}", res.getStatusCode());
                        handleRateLimits(res.getStatusCode().value());
                    })
                    .body(JsonNode.class);

            return extractFileId(response);
        } catch (OpenSubtitlesQuotaExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error searching subtitles (imdb={}, tmdb={}, lang={}): {}",
                    imdbId, tmdbId, lang, e.getMessage());
            return null;
        }
    }

    private String extractFileId(JsonNode response) {
        if (response != null && response.has("data")
                && response.get("data").isArray()
                && !response.get("data").isEmpty()) {

            // Берем первый элемент (предполагается, что ты уже добавил order_by=download_count в запрос)
            JsonNode firstResult = response.get("data").get(0);
            JsonNode attributes = firstResult.has("attributes") ? firstResult.get("attributes") : null;

            if (attributes != null && attributes.has("files")) {
                JsonNode files = attributes.get("files");

                if (files.isArray() && !files.isEmpty()) {
                    JsonNode firstFile = files.get(0);
                    String fileId = firstFile.has("file_id") ? firstFile.get("file_id").asText() : null;

                    if (fileId != null) {
                        // Извлекаем метаданные для логов
                        String fileName = firstFile.has("file_name") ? firstFile.get("file_name").asText() : "unknown";
                        String release = attributes.has("release") ? attributes.get("release").asText() : "unknown";
                        String subtitleId = attributes.has("subtitle_id") ? attributes.get("subtitle_id").asText() : "unknown";
                        String downloadCount = attributes.has("download_count") ? attributes.get("download_count").asText() : "0";

                        // Логируем полную картину
                        log.info("Selected Subtitle -> Subtitle ID: {}, File ID: {}, Downloads: {}, Release: '{}', File Name: '{}'",
                                subtitleId, fileId, downloadCount, release, fileName);

                        return fileId;
                    }
                }
            }
        }

        log.warn("No valid files found in OpenSubtitles response.");
        return null;
    }
    // ── Скачивание (расходует квоту 20/день!) ────────────────────────────

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

    // ── Обработка ошибок ─────────────────────────────────────────────────

    private void handleRateLimits(int statusCode) {
        if (statusCode == 406 || statusCode == 429) {
            throw new OpenSubtitlesQuotaExceededException(
                    "OpenSubtitles quota exhausted! Status code: " + statusCode);
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