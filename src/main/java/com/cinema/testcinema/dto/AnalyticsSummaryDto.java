package com.cinema.testcinema.dto;

import java.util.List;

public record AnalyticsSummaryDto(
        long totalSeconds,
        List<Item> genresPie,
        List<Point> activityByDay
) {
    public record Item(String label, long value) {}
    public record Point(String day, long seconds) {}
}
