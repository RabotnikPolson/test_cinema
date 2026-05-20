package com.cinema.testcinema.dto.analytics;

public record SubtitleQueueDto(
        long pending,
        long inProgress,
        long success,
        long failed,
        long total
) {}