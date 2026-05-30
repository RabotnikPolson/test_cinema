package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.analytics.AdminDashboardDto;
import com.cinema.testcinema.dto.analytics.SubtitleQueueRowDto;
import com.cinema.testcinema.service.AdminAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Analytics", description = "Глобальная аналитика платформы (только ADMIN)")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;
    private final JdbcTemplate jdbc;

    public AdminAnalyticsController(AdminAnalyticsService analyticsService, JdbcTemplate jdbc) {
        this.analyticsService = analyticsService;
        this.jdbc = jdbc;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Полный дашборд аналитики")
    public AdminDashboardDto getDashboard(
            @RequestParam(defaultValue = "10") int topLimit,
            @RequestParam(defaultValue = "week") String period
    ) {
        int safeLimit = Math.min(Math.max(topLimit, 1), 100);
        return analyticsService.getDashboard(safeLimit, period);
    }

    @GetMapping("/subtitle-queue")
    @Operation(summary = "Список фильмов в очереди субтитров (is_downloaded=true)")
    public List<SubtitleQueueRowDto> getSubtitleQueue() {
        return analyticsService.getSubtitleQueueRows();
    }

    @GetMapping("/rec-stats")
    @Operation(summary = "Статистика показов рекомендаций по стратегиям")
    public Map<String, Object> getRecStats() {
        List<Map<String, Object>> strategies = jdbc.queryForList("""
                SELECT strategy,
                       COUNT(*)                   AS impressions,
                       COUNT(DISTINCT movie_id)   AS unique_movies,
                       COUNT(DISTINCT user_id)    AS unique_users,
                       ROUND(100.0 * SUM(CASE WHEN clicked THEN 1 ELSE 0 END)
                             / NULLIF(COUNT(*), 0), 1) AS ctr_percent
                FROM recommendation_impressions
                GROUP BY strategy
                ORDER BY impressions DESC
                """);
        long total = strategies.stream()
                .mapToLong(r -> ((Number) r.get("impressions")).longValue())
                .sum();
        return Map.of("strategies", strategies, "totalImpressions", total);
    }
}
