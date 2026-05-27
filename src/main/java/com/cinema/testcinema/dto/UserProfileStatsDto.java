package com.cinema.testcinema.dto;

import java.util.List;

public record UserProfileStatsDto(
        long totalSeconds,
        int completedMovies,
        int kazakhstanMovies,
        int currentStreak,
        String favoriteGenre,
        String favoriteDirector,
        List<String> achievements
) {}
