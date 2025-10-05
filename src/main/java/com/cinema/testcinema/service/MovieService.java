package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.MovieDto;
import com.cinema.testcinema.model.Genre;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.GenreRepository;
import com.cinema.testcinema.repository.MovieRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final OmdbService omdbService;

    public MovieService(MovieRepository movieRepository, GenreRepository genreRepository, OmdbService omdbService) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.omdbService = omdbService;
    }

    /**
     * Получить все фильмы.
     */
    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    /**
     * Получить фильм по ID.
     */
    public Optional<Movie> getMovieById(Long id) {
        return movieRepository.findById(id);
    }

    /**
     * Добавить фильм: автозаполнение из OMDB по imdbId, привязка к жанру.
     */
    public Movie addMovie(MovieDto movieDto) {
        if (movieDto.getImdbId() == null || movieDto.getImdbId().isEmpty()) {
            throw new RuntimeException("IMDB ID is required for auto-filling movie data");
        }

        // Автозаполнение из OMDB
        Movie movie = omdbService.getMovieFromOmdb(movieDto.getImdbId());
        if (movie == null) {
            throw new RuntimeException("Movie not found in OMDB with ID: " + movieDto.getImdbId());
        }

        // Перезапись из DTO, если нужно (fallback)
        if (movieDto.getTitle() != null) movie.setTitle(movieDto.getTitle());
        if (movieDto.getYear() != null) movie.setYear(movieDto.getYear());

        // Привязка жанра
        Optional<Genre> genreOpt = genreRepository.findById(movieDto.getGenreId());
        if (genreOpt.isPresent()) {
            movie.setGenre(genreOpt.get());
        } else {
            throw new RuntimeException("Genre not found with id: " + movieDto.getGenreId());
        }

        return movieRepository.save(movie);
    }
}