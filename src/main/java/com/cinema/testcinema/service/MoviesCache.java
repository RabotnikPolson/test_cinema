package com.cinema.testcinema.service;

import com.cinema.testcinema.model.Movie;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MoviesCache {

    private static final long TTL_MS = 24L * 60 * 60 * 1000;

    private volatile List<Movie> movies = null;
    private volatile long cachedAt = 0L;

    public List<Movie> get() {
        if (movies != null && System.currentTimeMillis() - cachedAt < TTL_MS) {
            return movies;
        }
        return null;
    }

    public void put(List<Movie> movies) {
        this.movies = movies;
        this.cachedAt = System.currentTimeMillis();
    }

    public void evict() {
        this.movies = null;
    }
}
