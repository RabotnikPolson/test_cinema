package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.AdminAnalyticsDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    @PersistenceContext
    private EntityManager em;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminAnalyticsDto> getAnalytics() {
        AdminAnalyticsDto dto = new AdminAnalyticsDto();

        // Total views
        Long totalViews = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM watch_history").getSingleResult();
        dto.setTotalViews(totalViews != null ? totalViews : 0);

        // Total users
        Long totalUsers = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM users").getSingleResult();
        dto.setTotalUsers(totalUsers != null ? totalUsers : 0);

        // Views trend (last 7 days)
        @SuppressWarnings("unchecked")
        List<Object[]> trendRows = em.createNativeQuery(
                "SELECT DATE(started_at) AS d, COUNT(*) AS cnt " +
                "FROM watch_history " +
                "WHERE started_at >= CURRENT_DATE - INTERVAL '6 days' " +
                "GROUP BY DATE(started_at) ORDER BY d").getResultList();
        List<AdminAnalyticsDto.ViewsTrendItem> trend = new ArrayList<>();
        for (Object[] row : trendRows) {
            trend.add(new AdminAnalyticsDto.ViewsTrendItem(row[0].toString(), ((Number) row[1]).longValue()));
        }
        dto.setViewsTrend(trend);

        // Subtitle languages
        @SuppressWarnings("unchecked")
        List<Object[]> langRows = em.createNativeQuery(
                "SELECT language, COUNT(*) FROM movie_subtitles GROUP BY language ORDER BY COUNT(*) DESC"
        ).getResultList();
        List<AdminAnalyticsDto.LanguageCount> langs = new ArrayList<>();
        for (Object[] row : langRows) {
            langs.add(new AdminAnalyticsDto.LanguageCount((String) row[0], ((Number) row[1]).longValue()));
        }
        dto.setSubtitleLanguages(langs);

        // Domestic vs Global
        Long domesticViews = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM watch_history wh " +
                "JOIN movies m ON m.id = wh.movie_id WHERE m.is_domestic = true"
        ).getSingleResult();
        Long globalViews = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM watch_history wh " +
                "JOIN movies m ON m.id = wh.movie_id WHERE m.is_domestic = false"
        ).getSingleResult();
        dto.setDomesticVsGlobal(new AdminAnalyticsDto.DomesticVsGlobal(
                domesticViews != null ? domesticViews : 0,
                globalViews != null ? globalViews : 0));

        // Top search queries (placeholder)
        dto.setTopSearchQueries(List.of(
                new AdminAnalyticsDto.SearchQueryItem("Бауырына салу", 342),
                new AdminAnalyticsDto.SearchQueryItem("Marvel", 281),
                new AdminAnalyticsDto.SearchQueryItem("Комедия", 195),
                new AdminAnalyticsDto.SearchQueryItem("Казахстан", 167),
                new AdminAnalyticsDto.SearchQueryItem("Action 2024", 134)
        ));

        return ResponseEntity.ok(dto);
    }
}
