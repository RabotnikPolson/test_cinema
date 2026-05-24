package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.analytics.AdminDashboardDto;
import com.cinema.testcinema.service.AdminAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Analytics", description = "Глобальная аналитика платформы (только ADMIN)")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;

    public AdminAnalyticsController(AdminAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
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
}