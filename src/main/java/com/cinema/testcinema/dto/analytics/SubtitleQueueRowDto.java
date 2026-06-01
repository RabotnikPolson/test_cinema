package com.cinema.testcinema.dto.analytics;

public record SubtitleQueueRowDto(
        Long id,
        Long movieId,
        String movieTitle,
        String language,
        String status,
        String createdAt,
        Integer linesTranslated
) {}
