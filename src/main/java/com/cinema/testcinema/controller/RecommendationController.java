package com.cinema.testcinema.controller;

import com.cinema.testcinema.client.AiRecommendationClient;
import com.cinema.testcinema.security.AuthenticatedUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@Tag(name = "Recommendations", description = "AI-рекомендации через Java proxy")
public class RecommendationController {

    private final AiRecommendationClient aiClient;
    private final AuthenticatedUserService authService;
    private final JdbcTemplate jdbc;

    public RecommendationController(AiRecommendationClient aiClient,
                                    AuthenticatedUserService authService,
                                    JdbcTemplate jdbc) {
        this.aiClient = aiClient;
        this.authService = authService;
        this.jdbc = jdbc;
    }

    @GetMapping("/trending")
    @Operation(summary = "Тренды (публичный)")
    public Map<String, Object> trending(@RequestParam(defaultValue = "false") boolean weekly) {
        return aiClient.getTrending(weekly);
    }

    @GetMapping("/kazakhstan")
    @Operation(summary = "Казахстанское кино (публичный)")
    public Map<String, Object> kazakhstan(@RequestParam(defaultValue = "20") int limit,
                                          @RequestParam(required = false) String genre,
                                          @RequestParam(required = false) Integer yearFrom,
                                          @RequestParam(required = false) Integer yearTo,
                                          @RequestParam(required = false) String sortBy,
                                          Authentication authentication) {
        Long userId = authService.getCurrentUserIdIfAuthenticated(authentication);
        Map<String, Object> result = aiClient.getKazakhstan(userId, limit, genre, yearFrom, yearTo, sortBy);
        saveImpressions(userId, result, "kazakhstan");
        return result;
    }

    @GetMapping("/kazakhstan/genres")
    @Operation(summary = "Жанры казахстанского кино (публичный)")
    public Map<String, Object> kazakhstanGenres() {
        return aiClient.getKazakhstanGenres();
    }

    @GetMapping("/movie/{movieId}")
    @Operation(summary = "Похожие фильмы / правая колонка (публичный)")
    public Map<String, Object> rightRail(@PathVariable Long movieId,
                                         @RequestParam(defaultValue = "15") int limit,
                                         Authentication authentication) {
        Long userId = authService.getCurrentUserIdIfAuthenticated(authentication);
        Map<String, Object> result = aiClient.getRightRail(movieId, userId, limit);
        saveImpressions(userId, result, "smart_hybrid");
        return result;
    }

    @GetMapping("/movie/{movieId}/tabs/{type}")
    @Operation(summary = "Табы на странице фильма (публичный)")
    public Map<String, Object> tabs(@PathVariable Long movieId,
                                    @PathVariable String type,
                                    @RequestParam(defaultValue = "15") int limit,
                                    Authentication authentication) {
        Long userId = authService.getCurrentUserIdIfAuthenticated(authentication);
        Map<String, Object> result = aiClient.getTabRecommendations(type, movieId, userId, limit);
        saveImpressions(userId, result, type);
        return result;
    }

    @GetMapping("/feed")
    @Operation(summary = "Персональный фид (только авторизованным)")
    public Map<String, Object> feed(@RequestParam(defaultValue = "10") int limit,
                                    Authentication authentication) {
        Long userId = authService.requireCurrentUserId(authentication);
        return aiClient.getFeed(userId, limit);
    }

    @GetMapping("/because-you-liked")
    @Operation(summary = "Потому что вам понравилось (только авторизованным)")
    public Map<String, Object> becauseYouLiked(@RequestParam(defaultValue = "15") int limit,
                                                Authentication authentication) {
        Long userId = authService.requireCurrentUserId(authentication);
        Map<String, Object> result = aiClient.getBecauseYouLiked(userId, limit);
        saveImpressions(userId, result, "because_you_liked");
        return result;
    }

    @PostMapping("/retrain")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Запустить переобучение ML-модели (только ADMIN)")
    public Map<String, Object> retrain() {
        return aiClient.retrain();
    }

    @SuppressWarnings("unchecked")
    private void saveImpressions(Long userId, Map<String, Object> result, String strategy) {
        if (userId == null) return;
        try {
            List<Map<String, Object>> recs = (List<Map<String, Object>>) result.get("recommendations");
            if (recs == null || recs.isEmpty()) return;
            for (int i = 0; i < recs.size(); i++) {
                Object movieIdObj = recs.get(i).get("movie_id");
                if (movieIdObj == null) continue;
                long movieId = ((Number) movieIdObj).longValue();
                jdbc.update(
                    "INSERT INTO recommendation_impressions (user_id, movie_id, strategy, position) VALUES (?, ?, ?, ?)",
                    userId, movieId, strategy, i
                );
            }
        } catch (Exception e) {
            // impression logging is non-critical
        }
    }
}