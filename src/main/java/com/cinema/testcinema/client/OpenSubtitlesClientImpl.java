package com.cinema.testcinema.client;

import com.cinema.testcinema.dto.subtitle.DownloadLinkDto;
import com.cinema.testcinema.dto.subtitle.SubtitleInfoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class OpenSubtitlesClientImpl implements OpenSubtitlesClient {

    private static final Set<String> SUPPORTED_FORMATS = Set.of("srt", "vtt");

    private final RestClient restClient;
    private final String username;
    private final String password;

    private volatile String currentToken = null;

    public OpenSubtitlesClientImpl(
            RestClient.Builder restClientBuilder,
            @Value("${opensubtitles.api-key:dummy-api-key}") String apiKey,
            @Value("${opensubtitles.username:dummy-user}") String username,
            @Value("${opensubtitles.password:dummy-pass}") String password) {

        this.username = username;
        this.password = password;

        this.restClient = restClientBuilder
                .baseUrl("https://api.opensubtitles.com/api/v1")
                .defaultHeader("Api-Key", apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "QazaqCinema v1.0")
                .build();
    }

    @Override
    public String login() {
        log.info("Logging into OpenSubtitles API...");
        Map<String, String> body = Map.of("username", username, "password", password);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/login")
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response != null && response.containsKey("token")) {
            currentToken = (String) response.get("token");
            log.info("Successfully logged into OpenSubtitles");
            return currentToken;
        }
        throw new RuntimeException("OpenSubtitles login: no token in response");
    }

    private String getToken() {
        if (currentToken == null) {
            login();
        }
        return currentToken;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SubtitleInfoDto> searchSubtitles(String imdbId, List<String> languages) {
        String langs = String.join(",", languages);
        log.info("Searching subtitles for IMDb {} in languages [{}]", imdbId, langs);

        try {
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/subtitles")
                            .queryParam("imdb_id", imdbId)
                            .queryParam("languages", langs)
                            .queryParam("order_by", "ratings")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + getToken())
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("data")) {
                return List.of();
            }

            List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
            List<SubtitleInfoDto> result = new ArrayList<>();

            for (Map<String, Object> item : dataList) {
                Map<String, Object> attrs = (Map<String, Object>) item.get("attributes");
                if (attrs == null) continue;

                // --- Извлекаем file_id ---
                List<Map<String, Object>> files = (List<Map<String, Object>>) attrs.get("files");
                if (files == null || files.isEmpty()) continue;
                String fileIdStr = String.valueOf(files.get(0).get("file_id"));

                // --- Надежный парсинг формата (без хардкода "srt") ---
                String format = resolveFormat(attrs, files.get(0));

                // --- Рейтинг: API возвращает либо Double, либо Integer ---
                Object ratingRaw = attrs.getOrDefault("ratings", 0.0);
                double rating = ratingRaw instanceof Number ? ((Number) ratingRaw).doubleValue() : 0.0;

                result.add(new SubtitleInfoDto(
                        fileIdStr,
                        (String) item.get("id"),
                        (String) attrs.get("language"),
                        format,
                        rating));
            }

            log.info("Found {} subtitles for IMDb {}", result.size(), imdbId);
            return result;

        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("401 Unauthorized — token expired, invalidating. Next call will re-login.");
            this.currentToken = null;
            return List.of();
        } catch (HttpClientErrorException e) {
            log.error("HTTP {} error during subtitle search for IMDb {}", e.getStatusCode().value(), imdbId);
            return List.of();
        } catch (RestClientException e) {
            log.error("Network error during subtitle search for IMDb {}", imdbId, e);
            return List.of();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public DownloadLinkDto requestDownloadLink(String osFileId) {
        log.info("Requesting download link for osFileId={}", osFileId);
        Map<String, Object> body = Map.of("file_id", Integer.parseInt(osFileId));

        Map<String, Object> response = restClient.post()
                .uri("/download")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getToken())
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey("link")) {
            throw new RuntimeException("No download link in response for osFileId=" + osFileId);
        }

        String link = (String) response.get("link");
        // API возвращает "requests" as Integer
        Object reqRaw = response.getOrDefault("requests", 0);
        int remaining = reqRaw instanceof Number ? ((Number) reqRaw).intValue() : 0;

        log.info("Download link received. Remaining quota: {}", remaining);
        return new DownloadLinkDto(link, remaining);
    }

    /**
     * Определяет формат субтитров без хардкода "srt".
     * Приоритет: поле "format" в attributes → расширение из "file_name" в files[0] → null с предупреждением.
     */
    private String resolveFormat(Map<String, Object> attrs, Map<String, Object> fileEntry) {
        // 1. Приоритет — явное поле format в attributes
        Object formatAttr = attrs.get("format");
        if (formatAttr instanceof String s && !s.isBlank()) {
            return normalizeFormat(s);
        }

        // 2. Фолбэк — парсим расширение из file_name (например, "Movie.2014.srt")
        Object fileNameObj = fileEntry.get("file_name");
        if (fileNameObj instanceof String fileName) {
            int dot = fileName.lastIndexOf('.');
            if (dot >= 0 && dot < fileName.length() - 1) {
                String ext = fileName.substring(dot + 1).toLowerCase();
                return normalizeFormat(ext);
            }
        }

        log.warn("Cannot resolve subtitle format from API response. attrs.format={}, file_name={}",
                attrs.get("format"), fileEntry.get("file_name"));
        return null; // Явный null — вызывающий код решит, что делать
    }

    private String normalizeFormat(String raw) {
        String fmt = raw.toLowerCase().trim();
        if (!SUPPORTED_FORMATS.contains(fmt)) {
            log.warn("Unsupported subtitle format '{}'. Only srt/vtt are expected by the pipeline.", fmt);
        }
        return fmt;
    }
}
