package com.cinema.testcinema.controller;

import com.cinema.testcinema.security.AuthenticatedUserService;
import com.cinema.testcinema.service.AiServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/recommendations")
@Tag(name = "Recommendations", description = "AI Recommendations powered by FastAPI ML Service")
public class RecommendationController {

    private final AiServiceClient aiServiceClient;
    private final AuthenticatedUserService authenticatedUserService;

    public RecommendationController(AiServiceClient aiServiceClient,
                                    AuthenticatedUserService authenticatedUserService) {
        this.aiServiceClient = aiServiceClient;
        this.authenticatedUserService = authenticatedUserService;
    }

    // ─── Smart Feed (главная страница) ─────────────────────────────────────────

    @GetMapping("/feed")
    @Operation(summary = "YouTube-подобная лента рекомендаций для текущего пользователя")
    public ResponseEntity<Map<String, Object>> getSmartFeed(Authentication authentication) {
        Long userId = authenticatedUserService.requireCurrentUserId(authentication);
        try {
            Map<String, Object> feed = aiServiceClient.getSmartFeed(userId);
            return ResponseEntity.ok(feed);
        } catch (RuntimeException e) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "Сервис рекомендаций временно недоступен",
                "detail", e.getMessage()
            ));
        }
    }

    // ─── Tab рекомендации (страница фильма) ────────────────────────────────────

    @GetMapping("/tab/{type}/{movieId}")
    @Operation(summary = "Рекомендации по типу для конкретного фильма (franchise, director, actor, genre, content, hybrid, smart)")
    public ResponseEntity<Map<String, Object>> getTabRecommendations(
            @PathVariable String type,
            @PathVariable Long movieId,
            @RequestParam(defaultValue = "15") int limit,
            Authentication authentication) {
        Long userId = authenticatedUserService.getCurrentUserIdIfAuthenticated(authentication);
        try {
            Map<String, Object> result = switch (type) {
                case "franchise"           -> aiServiceClient.getFranchiseRecommendations(movieId, limit);
                case "director"            -> aiServiceClient.getDirectorRecommendations(movieId, limit);
                case "actor"               -> aiServiceClient.getActorRecommendations(movieId, limit);
                case "genre"               -> aiServiceClient.getGenreRecommendations(movieId, limit);
                case "content"             -> aiServiceClient.getContentRecommendations(movieId, limit);
                case "hybrid"              -> aiServiceClient.getHybridRecommendations(movieId, limit);
                case "smart"               -> aiServiceClient.getSmartRecommendations(movieId, limit, userId);
                case "collaborative-item"  -> aiServiceClient.getCollaborativeItemRecommendations(movieId, limit);
                default -> throw new IllegalArgumentException("Неизвестный тип рекомендаций: " + type);
            };
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "Сервис рекомендаций временно недоступен",
                "detail", e.getMessage()
            ));
        }
    }

    // ─── Domestic (казахстанское кино) ─────────────────────────────────────────

    @GetMapping("/tab/domestic")
    @Operation(summary = "Казахстанское кино")
    public ResponseEntity<Map<String, Object>> getDomesticRecommendations(
            @RequestParam(defaultValue = "15") int limit) {
        try {
            return ResponseEntity.ok(aiServiceClient.getDomesticRecommendations(limit));
        } catch (RuntimeException e) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "Сервис рекомендаций временно недоступен",
                "detail", e.getMessage()
            ));
        }
    }

    // ─── Because You Liked ─────────────────────────────────────────────────────

    @GetMapping("/tab/because-you-liked")
    @Operation(summary = "Рекомендации на основе высоко оценённых фильмов")
    public ResponseEntity<Map<String, Object>> getBecauseYouLiked(
            @RequestParam(defaultValue = "15") int limit,
            Authentication authentication) {
        Long userId = authenticatedUserService.requireCurrentUserId(authentication);
        try {
            return ResponseEntity.ok(aiServiceClient.getBecauseYouLikedRecommendations(userId, limit));
        } catch (RuntimeException e) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "Сервис рекомендаций временно недоступен",
                "detail", e.getMessage()
            ));
        }
    }

    // ─── Right Rail (похожие, старый endpoint) ─────────────────────────────────

    @GetMapping("/movie/{movieId}")
    @Operation(summary = "Гибридные рекомендации для правой колонки страницы фильма")
    public ResponseEntity<Map<String, Object>> getRightRailRecommendations(
            @PathVariable Long movieId,
            @RequestParam(defaultValue = "15") int limit,
            Authentication authentication) {
        Long userId = authenticatedUserService.getCurrentUserIdIfAuthenticated(authentication);
        try {
            return ResponseEntity.ok(aiServiceClient.getSmartRecommendations(movieId, limit, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "Сервис рекомендаций временно недоступен",
                "detail", e.getMessage()
            ));
        }
    }
}
