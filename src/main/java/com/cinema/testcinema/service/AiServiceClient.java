package com.cinema.testcinema.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP-клиент для взаимодействия с Python AI микросервисом (FastAPI / uvicorn).
 * По умолчанию сервис работает на http://localhost:8000
 */
@Service
public class AiServiceClient {

    private final RestTemplate restTemplate;
    private final String aiServiceUrl;

    public AiServiceClient(
            RestTemplate restTemplate,
            @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl) {
        this.restTemplate = restTemplate;
        this.aiServiceUrl = aiServiceUrl;
    }

    // ─── Tab рекомендации ──────────────────────────────────────────────────────

    public Map<String, Object> getFranchiseRecommendations(Long movieId, int limit) {
        return getForMap("/api/v1/recommend/tab/franchise/" + movieId + "?limit=" + limit);
    }

    public Map<String, Object> getDirectorRecommendations(Long movieId, int limit) {
        return getForMap("/api/v1/recommend/tab/director/" + movieId + "?limit=" + limit);
    }

    public Map<String, Object> getActorRecommendations(Long movieId, int limit) {
        return getForMap("/api/v1/recommend/tab/actor/" + movieId + "?limit=" + limit);
    }

    public Map<String, Object> getGenreRecommendations(Long movieId, int limit) {
        return getForMap("/api/v1/recommend/tab/genre/" + movieId + "?limit=" + limit);
    }

    public Map<String, Object> getContentRecommendations(Long movieId, int limit) {
        return getForMap("/api/v1/recommend/tab/content/" + movieId + "?limit=" + limit);
    }

    public Map<String, Object> getHybridRecommendations(Long movieId, int limit) {
        return getForMap("/api/v1/recommend/tab/hybrid/" + movieId + "?limit=" + limit);
    }

    public Map<String, Object> getSmartRecommendations(Long movieId, int limit, Long userId) {
        String url = "/api/v1/recommend/tab/smart/" + movieId + "?limit=" + limit;
        if (userId != null) url += "&user_id=" + userId;
        return getForMap(url);
    }

    public Map<String, Object> getCollaborativeItemRecommendations(Long movieId, int limit) {
        return getForMap("/api/v1/recommend/tab/collaborative-item/" + movieId + "?limit=" + limit);
    }

    public Map<String, Object> getDomesticRecommendations(int limit) {
        return getForMap("/api/v1/recommend/tab/domestic?limit=" + limit);
    }

    public Map<String, Object> getBecauseYouLikedRecommendations(Long userId, int limit) {
        return getForMap("/api/v1/recommend/tab/because-you-liked/" + userId + "?limit=" + limit);
    }

    // ─── Совместимость (старые эндпоинты) ─────────────────────────────────────

    public Map<String, Object> getCollaborativeRecommendations(Long userId, int limit) {
        return getForMap("/api/v1/recommend/collaborative/" + userId + "?limit=" + limit);
    }

    public Map<String, Object> getSmartFeed(Long userId) {
        return getForMap("/api/v1/recommend/feed/" + userId);
    }

    // ─── Утилита ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> getForMap(String path) {
        String url = aiServiceUrl + path;
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody() != null ? response.getBody() : Map.of();
        } catch (ResourceAccessException e) {
            throw new RuntimeException("AI сервис недоступен. Убедитесь что Python uvicorn запущен на " + aiServiceUrl, e);
        }
    }
}
