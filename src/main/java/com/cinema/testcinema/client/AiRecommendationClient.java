package com.cinema.testcinema.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Map;

@Component
public class AiRecommendationClient {

    private static final Logger log = LoggerFactory.getLogger(AiRecommendationClient.class);

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    private final RestTemplate restTemplate;

    public AiRecommendationClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restTemplate = new RestTemplate(factory);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTabRecommendations(String type, Long movieId, Long userId, int limit) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(aiServiceUrl + "/api/v1/recommendations/tab/{type}/{movieId}")
                    .queryParam("limit", limit)
                    .queryParamIfPresent("user_id", java.util.Optional.ofNullable(userId))
                    .buildAndExpand(type, movieId)
                    .toUriString();
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.warn("AI tab recommendations failed [{}/{}]: {}", type, movieId, e.getMessage());
            return Map.of("recommendations", java.util.List.of(), "method", type, "total", 0);
        }
    }

    @Cacheable(value = "rec_kazakhstan", key = "#limit")
    @SuppressWarnings("unchecked")
    public Map<String, Object> getKazakhstan(Long userId, int limit) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(aiServiceUrl + "/api/v1/recommendations/tab/kazakhstan")
                    .queryParam("limit", limit)
                    .queryParamIfPresent("user_id", java.util.Optional.ofNullable(userId))
                    .toUriString();
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.warn("AI kazakhstan recommendations failed: {}", e.getMessage());
            return Map.of("recommendations", java.util.List.of(), "method", "kazakhstan", "total", 0);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getRightRail(Long movieId, Long userId, int limit) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(aiServiceUrl + "/api/v1/recommendations/movie/{movieId}")
                    .queryParam("limit", limit)
                    .queryParamIfPresent("user_id", java.util.Optional.ofNullable(userId))
                    .buildAndExpand(movieId)
                    .toUriString();
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.warn("AI right rail failed [{}]: {}", movieId, e.getMessage());
            return Map.of("recommendations", java.util.List.of(), "method", "smart_hybrid");
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getFeed(Long userId, int limit) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(aiServiceUrl + "/api/v1/recommendations/feed")
                    .queryParam("user_id", userId)
                    .queryParam("limit", limit)
                    .toUriString();
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.warn("AI feed failed [user={}]: {}", userId, e.getMessage());
            return Map.of("feed", Map.of());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getBecauseYouLiked(Long userId, int limit) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(aiServiceUrl + "/api/v1/recommendations/tab/because-you-liked")
                    .queryParam("user_id", userId)
                    .queryParam("limit", limit)
                    .toUriString();
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.warn("AI because-you-liked failed [user={}]: {}", userId, e.getMessage());
            return Map.of("recommendations", java.util.List.of(), "method", "because_you_liked", "total", 0);
        }
    }

    public Map<String, Object> retrain() {
        try {
            return restTemplate.postForObject(aiServiceUrl + "/api/v1/ml/retrain", null, Map.class);
        } catch (Exception e) {
            log.warn("AI retrain failed: {}", e.getMessage());
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    @Cacheable(value = "rec_trending", key = "#weekly")
    @SuppressWarnings("unchecked")
    public Map<String, Object> getTrending(boolean weekly) {
        try {
            String path = weekly ? "/api/v1/trending/weekly" : "/api/v1/trending";
            return restTemplate.getForObject(aiServiceUrl + path, Map.class);
        } catch (Exception e) {
            log.warn("AI trending failed: {}", e.getMessage());
            return Map.of("recommendations", java.util.List.of(), "method", "trending", "total", 0);
        }
    }
}