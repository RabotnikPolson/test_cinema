package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.metrics.MovieClickRequest;
import com.cinema.testcinema.dto.metrics.SearchLogRequest;
import com.cinema.testcinema.dto.metrics.SubtitleEventRequest;
import com.cinema.testcinema.security.AuthenticatedUserService;
import com.cinema.testcinema.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/metrics")
@Tag(name = "Metrics", description = "Сбор событий пользователей для AI-рекомендаций")
public class MetricsController {

    private final MetricsService metricsService;
    private final AuthenticatedUserService authService;

    public MetricsController(MetricsService metricsService, AuthenticatedUserService authService) {
        this.metricsService = metricsService;
        this.authService = authService;
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
}
