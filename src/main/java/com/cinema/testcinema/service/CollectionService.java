package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.movie.TrendingMovieDto;
import com.cinema.testcinema.model.Movie;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CollectionService {

    private final MovieService movieService;

    public CollectionService(MovieService movieService) {
        this.movieService = movieService;
    }

    @Cacheable(value = "home_collections", key = "'new_releases'")
    public List<TrendingMovieDto> getNewReleases() {
        // Тяжелый запрос: Фильмы текущего года, сортировка по убыванию даты или рейтинга
        List<Movie> movies = movieService.searchMovies(null, null, 2024L, 2026L, 0, 10);
        return mapToDto(movies);
    }

    @Cacheable(value = "home_collections", key = "'top_comedies'")
    public List<TrendingMovieDto> getTopComedies() {
        // Допустим, жанр комедия имеет ID = 5
        List<Movie> movies = movieService.searchMovies(null, 5L, null, null, 0, 10);
        return mapToDto(movies);
    }

    private List<TrendingMovieDto> mapToDto(List<Movie> movies) {
        return movies.stream()
                .map(m -> new TrendingMovieDto(
                        m.getId(),
                        m.getTitle(),
                        m.getPosterUrl(),
                        m.getRatingKinopoisk() != null ? m.getRatingKinopoisk().doubleValue() : 0.0,
                        m.isDomestic()
                ))
                .collect(Collectors.toList());
    }
}
