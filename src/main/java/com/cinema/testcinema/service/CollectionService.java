package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.movie.TrendingMovieDto;
import com.cinema.testcinema.model.Genre;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.GenreRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CollectionService {

    private final MovieService movieService;
    private final GenreRepository genreRepository;

    public CollectionService(MovieService movieService, GenreRepository genreRepository) {
        this.movieService = movieService;
        this.genreRepository = genreRepository;
    }

    @Cacheable(value = "home_collections", key = "'new_releases'")
    public List<TrendingMovieDto> getNewReleases() {
        long currentYear = Year.now().getValue();
        List<Movie> movies = movieService.searchMovies(null, null, currentYear - 1, currentYear + 1, 0, 10);
        return mapToDto(movies);
    }

    @Cacheable(value = "home_collections", key = "'top_comedies'")
    public List<TrendingMovieDto> getTopComedies() {
        Genre comedy = genreRepository.findByName("Комедия");
        if (comedy == null) return List.of();
        List<Movie> movies = movieService.searchMovies(null, comedy.getId(), null, null, 0, 10);
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
