package com.cinema.testcinema.dto.analytics;

public record SubtitleQueueRowDto(
        Long id,
        String movieTitle,
        String language,
        String status,
        String createdAt,
        Integer linesTranslated
) {}
