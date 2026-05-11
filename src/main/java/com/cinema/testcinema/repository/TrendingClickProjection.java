package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.Movie;

public interface TrendingClickProjection {
    Movie getMovie();
    Long getClicks();
}
