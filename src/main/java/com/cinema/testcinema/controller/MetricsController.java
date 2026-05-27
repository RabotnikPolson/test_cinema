package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.metrics.MovieClickRequest;
import com.cinema.testcinema.dto.metrics.RecClickRequest;
import com.cinema.testcinema.dto.metrics.SearchLogRequest;
import com.cinema.testcinema.dto.metrics.SubtitleEventRequest;
import com.cinema.testcinema.security.AuthenticatedUserService;
import com.cinema.testcinema.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/metrics")
@Tag(name = "Metrics", description = "Сбор событий пользователей для AI-рекомендаций")
public class MetricsController {

    private final MetricsService metricsService;
    private final AuthenticatedUserService authService;
    private final JdbcTemplate jdbc;

    public MetricsController(MetricsService metricsService, AuthenticatedUserService authService, JdbcTemplate jdbc) {
        this.metricsService = metricsService;
        this.authService = authService;
        this.jdbc = jdbc;
    }

    @PostMapping("/clicks")
    @Operation(summary = "Залогировать клик по карточке фильма")
    public ResponseEntity<Void> logClick(@Valid @RequestBody MovieClickRequest request,
                                         Authentication authentication) {
        Long userId = authService.getCurrentUserIdIfAuthenticated(authentication);
        metricsService.logClick(request, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/searches")
    @Operation(summary = "Залогировать поисковый запрос")
    public ResponseEntity<Void> logSearch(@Valid @RequestBody SearchLogRequest request,
                                          Authentication authentication) {
        Long userId = authService.getCurrentUserIdIfAuthenticated(authentication);
        metricsService.logSearch(request, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/subtitles")
    @Operation(summary = "Залогировать событие субтитров (для всех)")
    public ResponseEntity<Void> logSubtitleEvent(@Valid @RequestBody SubtitleEventRequest request,
                                                  Authentication authentication) {
        Long userId = authService.getCurrentUserIdIfAuthenticated(authentication);
        metricsService.logSubtitleEvent(request, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rec-click")
    @Operation(summary = "Залогировать клик по рекомендации (обновляет CTR в recommendation_impressions)")
    public ResponseEntity<Void> logRecClick(@Valid @RequestBody RecClickRequest request,
                                             Authentication authentication) {
        Long userId = authService.getCurrentUserIdIfAuthenticated(authentication);
        if (userId == null) return ResponseEntity.noContent().build();
        jdbc.update("""
                UPDATE recommendation_impressions
                SET clicked = true, clicked_at = now()
                WHERE id = (
                    SELECT id FROM recommendation_impressions
                    WHERE user_id = ? AND movie_id = ? AND strategy = ? AND clicked = false
                    ORDER BY shown_at DESC
                    LIMIT 1
                )
                """, userId, request.movieId(), request.strategy());
        return ResponseEntity.noContent().build();
    }
}
