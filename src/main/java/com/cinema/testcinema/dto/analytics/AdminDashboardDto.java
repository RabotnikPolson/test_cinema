package com.cinema.testcinema.dto.analytics;

import java.util.List;

public record AdminDashboardDto(
        AdminOverviewDto overview,
        List<TopMovieDto> topByClicks,
        List<TopMovieDto> topByWatchTime,
        List<TopMovieDto> topRatedMovies,
        List<TopMovieDto> topDomesticByClicks,
        ContentRatioDto contentRatio,
        SubtitleQueueDto subtitleQueue
) {}