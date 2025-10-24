package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.MovieDto;
import com.cinema.testcinema.model.Genre;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.GenreRepository;
import com.cinema.testcinema.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MovieService {

    private static final Logger logger = LoggerFactory.getLogger(MovieService.class);

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final OmdbService omdbService;

    public MovieService(MovieRepository movieRepository, GenreRepository genreRepository, OmdbService omdbService) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.omdbService = omdbService;
    }

    /**
     * Получить все фильмы по типу (movie, series, anime).
     */
    @Transactional(readOnly = true)
    public List<Movie> getAllMoviesByType(String type) {
        logger.info("Fetching all movies of type: {}", type);
        return movieRepository.findByType(type.toLowerCase());
    }

    /**
     * Получить фильм по ID.
     */
    @Transactional(readOnly = true)
    public Optional<Movie> getMovieById(Long id) {
        logger.info("Fetching movie by ID: {}", id);
        return movieRepository.findById(id);
    }

    /**
     * Добавить новый фильм с проверками и логами.
     */
    @Transactional
    public Movie addMovie(MovieDto movieDto) {
        logger.info("Attempting to add movie with IMDB ID: {}", movieDto.getImdbId());

        if (movieDto.getImdbId() == null || movieDto.getImdbId().trim().isEmpty()) {
            throw new IllegalArgumentException("IMDB ID is required");
        }

        Movie movie = omdbService.getMovieFromOmdb(movieDto.getImdbId());
        if (movie == null) {
            logger.error("Movie not found in OMDB for IMDB ID: {}", movieDto.getImdbId());
            throw new RuntimeException("Movie not found in OMDB");
        }

        // Обновляем поля из DTO
        movie.setTitle(movieDto.getTitle() != null ? movieDto.getTitle() : movie.getTitle());
        movie.setYear(movieDto.getYear() != null ? movieDto.getYear() : movie.getYear());
        movie.setType(movieDto.getType() != null ? movieDto.getType().toLowerCase() : movie.getType());

        // Устанавливаем жанр, если указан
        if (movieDto.getGenreId() != null) {
            Optional<Genre> genreOpt = genreRepository.findById(movieDto.getGenreId());
            genreOpt.ifPresent(movie::setGenre);
        }

        // Сохраняем и логируем результат
        Movie savedMovie = movieRepository.save(movie);
        logger.info("Successfully added movie with ID: {}", savedMovie.getId());
        return savedMovie;
    }
}