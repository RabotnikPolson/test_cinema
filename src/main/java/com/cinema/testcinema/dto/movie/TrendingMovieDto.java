package com.cinema.testcinema.dto.movie;

public record TrendingMovieDto(
        Long movieId,
        String title,
        String posterUrl,
        Double score,
        boolean isDomestic
) {}
