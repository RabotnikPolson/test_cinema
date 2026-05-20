package com.cinema.testcinema.dto.analytics;

public record AdminOverviewDto(
        long totalMovies,
        long domesticMovies,
        long foreignMovies,
        long totalUsers,
        long totalWatchHours,
        long translatedSubtitles
) {}
