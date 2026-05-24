package com.cinema.testcinema.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP-клиент к Kinopoisk Unofficial API (kinopoisk.dev / unofficialapi).
 * Документация: kinopoisk_openapi.json в проекте.
 *
 * Лимиты FREE-аккаунта: 500 req/day, 20 req/sec.
 * Все данные кэшируются в БД при первом запросе (см. KinopoiskSyncService).
 */
@Service
public class KinopoiskClient {

    private static final String BASE_URL = "https://kinopoiskapiunofficial.tech";

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String apiKey;

    public KinopoiskClient(@Value("${kinopoisk.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    // ─── Фильм ───────────────────────────────────────────────────────────────

    /**
     * GET /api/v2.2/films/{id} — основные метаданные фильма.
     */
    @Cacheable(value = "kinopoisk_film", key = "#kinopoiskId")
    public JsonNode fetchFilm(String kinopoiskId) {
        return get("/api/v2.2/films/" + kinopoiskId);
    }

    /**
     * GET /api/v2.2/films?keyword={keyword} — поиск фильма по ключевому слову.
     */
    @Cacheable(value = "kinopoisk_search", key = "#keyword")
    public JsonNode searchFilms(String keyword) {
        String query = "?keyword=" + encode(keyword);
        return get("/api/v2.2/films" + query);
    }

    // ─── Похожие и Тренды ─────────────────────────────────────────────────────

    /**
     * GET /api/v2.2/films/collections?type=TOP_POPULAR_ALL&page={page} — ТОП популярных
     */
    @Cacheable(value = "kinopoisk_top", key = "#page")
    public JsonNode fetchTopPopular(int page) {
        return get("/api/v2.2/films/collections?type=TOP_POPULAR_ALL&page=" + page);
    }

    /**
     * GET /api/v2.2/films/{id}/similars — список похожих фильмов (для AI-графа).
     */
    @Cacheable(value = "kinopoisk_similars", key = "#kinopoiskId")
    public JsonNode fetchSimilars(String kinopoiskId) {
        return get("/api/v2.2/films/" + kinopoiskId + "/similars");
    }

    // ─── Съёмочная группа ─────────────────────────────────────────────────────

    /**
     * GET /api/v1/staff?filmId={id} — актёры, режиссёры, продюсеры фильма.
     */
    @Cacheable(value = "kinopoisk_staff", key = "#kinopoiskId")
    public JsonNode fetchStaff(String kinopoiskId) {
        return get("/api/v1/staff?filmId=" + kinopoiskId);
    }

    // ─── AI-Companion контекст ────────────────────────────────────────────────

    /**
     * GET /api/v2.2/films/{id}/facts — интересные факты и ляпы (для AI RAG).
     */
    @Cacheable(value = "kinopoisk_facts", key = "#kinopoiskId")
    public JsonNode fetchFacts(String kinopoiskId) {
        return get("/api/v2.2/films/" + kinopoiskId + "/facts");
    }

    /**
     * GET /api/v2.2/films/{id}/reviews — рецензии критиков и зрителей (для AI RAG).
     */
    @Cacheable(value = "kinopoisk_reviews", key = "#kinopoiskId")
    public JsonNode fetchReviews(String kinopoiskId) {
        return get("/api/v2.2/films/" + kinopoiskId + "/reviews?page=1");
    }

    // ─── Внутренний HTTP вызов ────────────────────────────────────────────────

    private JsonNode get(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .header("X-API-KEY", apiKey)
                    .header("Content-Type", "application/json")
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 402) {
                throw new RuntimeException("Kinopoisk API: лимит запросов исчерпан (402). Повторите завтра.");
            }
            if (response.statusCode() == 401) {
                throw new RuntimeException("Kinopoisk API: неверный X-API-KEY (401).");
            }
            if (response.statusCode() != 200) {
                throw new RuntimeException("Kinopoisk API вернул статус " + response.statusCode() + " для: " + path);
            }

            return mapper.readTree(response.body());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Ошибка соединения с Kinopoisk API: " + e.getMessage(), e);
        }
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
