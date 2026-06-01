package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.MovieDto;
import com.cinema.testcinema.model.Genre;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.GenreRepository;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.MovieFilterRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MovieFilterRepository movieFilterRepository;
    private final MoviesCache moviesCache;

    public MovieService(MovieRepository movieRepository,
                        GenreRepository genreRepository,
                        MovieFilterRepository movieFilterRepository,
                        MoviesCache moviesCache) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.movieFilterRepository = movieFilterRepository;
        this.moviesCache = moviesCache;
    }

    public Movie addMovie(MovieDto movieDto) {
        Movie movie = new Movie();
        movie.setTitle(movieDto.getTitle());
        movie.setYear((long) movieDto.getYear());
        movie.setImdbId(movieDto.getImdbId());

        Optional<Genre> genreOpt = genreRepository.findById(movieDto.getGenreId());
        if (genreOpt.isPresent()) {
            Genre genre = genreOpt.get();
            movie.getGenres().add(genre);
        } else {
            throw new RuntimeException("Genre not found with id: " + movieDto.getGenreId());
        }

        return movieRepository.save(movie);
    }

    public List<Movie> searchMovies(String q,
                                    Long genreId,
                                    Long yearFrom,
                                    Long yearTo,
                                    Integer page,
                                    Integer size) {
        if (yearFrom != null && yearTo != null && yearFrom > yearTo) {
            throw new IllegalArgumentException("yearFrom must be less than or equal to yearTo");
        }
        if (page != null && page < 0) {
            throw new IllegalArgumentException("page must be non‑negative");
        }
        if (size != null && size <= 0) {
            throw new IllegalArgumentException("size must be greater than zero");
        }
        Integer effectivePage = page;
        Integer effectiveSize = size;
        if (effectivePage != null && effectiveSize == null) {
            effectiveSize = 20;
        }
        if (effectivePage == null && effectiveSize != null) {
            effectivePage = 0;
        }

        boolean isFullList = q == null && genreId == null && yearFrom == null && yearTo == null
                && effectivePage == null && effectiveSize == null;
        if (isFullList) {
            List<Movie> cached = moviesCache.get();
            if (cached != null) return cached;
        }

        List<Movie> movies = movieFilterRepository.searchMovies(q, genreId, yearFrom, yearTo, effectivePage, effectiveSize);
        movies.forEach(m -> m.setGenres(new HashSet<>(m.getGenres())));

        if (isFullList) moviesCache.put(movies);
        return movies;
    }

    @Cacheable(value = "movie_detail", key = "#id")
    public Movie getMovieById(Long id) {
        Movie movie = movieRepository.findWithGenresById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Фильм с ID " + id + " не найден"));
        movie.setGenres(new HashSet<>(movie.getGenres()));
        return movie;
    }

    @CacheEvict(value = "movie_detail", key = "#id")
    public void evictMovieCache(Long id) {
        moviesCache.evict();
    }

    public void evictMoviesListCache() {
        moviesCache.evict();
    }
}
