package com.cinema.testcinema.dto.analytics;

public record ContentRatioDto(
        long domesticWatchSeconds,
        long foreignWatchSeconds,
        double domesticPercent,
        double foreignPercent
) {}