// src/main/java/com/cinema/testcinema/controller/AdminAnalyticsController.java
package com.cinema.testcinema.controller;

import com.cinema.testcinema.model.AnalyticsDaily;
import com.cinema.testcinema.repository.AnalyticsDailyRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {
    private final AnalyticsDailyRepository repo;

    public AdminAnalyticsController(AnalyticsDailyRepository repo) { this.repo = repo; }

    @GetMapping("/daily")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AnalyticsDaily> daily() {
        return repo.findAll();
    }
}
