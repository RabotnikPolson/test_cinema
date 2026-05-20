package com.cinema.testcinema.dto.analytics;

public record TopMovieDto(
        Long movieId,
        String title,
        String posterUrl,
        boolean isDomestic,
        long score
) {}