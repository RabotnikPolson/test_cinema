package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.MovieDto;
import com.cinema.testcinema.model.Genre;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.service.MovieStreamService;
import com.cinema.testcinema.repository.GenreRepository;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.service.OmdbService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final OmdbService omdbService;
    private final MovieStreamService movieStreamService;

    public MovieController(MovieRepository movieRepository,
                           GenreRepository genreRepository,
                           OmdbService omdbService,
                           MovieStreamService movieStreamService) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.omdbService = omdbService;
        this.movieStreamService = movieStreamService;
    }

    // старый эндпоинт, оставляем
    @GetMapping("/imdb/{imdbId}")
    public Movie getMovieByImdbId(@PathVariable String imdbId) {
        return findOrCreateMovieByImdbId(imdbId);
    }

    // НОВЫЙ эндпоинт для страницы /movie/:imdbId/watch
    @GetMapping("/imdb/{imdbId}/watch")
    public MovieDto getMovieForWatch(@PathVariable String imdbId) {
        Movie movie = findOrCreateMovieByImdbId(imdbId);
        return toWatchDto(movie);
    }

    @PostMapping("/addFromImdb")
    public Movie addFromImdb(@RequestParam String imdbId) {
        Movie existing = movieRepository.findByImdbId(imdbId).orElse(null);
        if (existing != null) return existing;

        Movie movie = omdbService.getMovieFromOmdb(imdbId);
        if (movie == null) {
            throw new RuntimeException("Фильм не найден в OMDB API");
        }

        String genreText = movie.getGenreText() == null ? "" : movie.getGenreText();
        String firstGenreName = genreText.isBlank()
                ? "Unknown"
                : (genreText.contains(",") ? genreText.split(",")[0].trim() : genreText.trim());

        Genre genre = genreRepository.findByName(firstGenreName);
        if (genre == null) {
            genre = new Genre();
            genre.setName(firstGenreName);
            genre = genreRepository.save(genre);
        }

        movie.setGenre(genre);
        return movieRepository.save(movie);
    }

    @GetMapping
    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    @GetMapping("/{id}")
    public Movie getMovieById(@PathVariable Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Фильм с ID " + id + " не найден"));
    }

    // ===== внутренняя логика =====

    private Movie findOrCreateMovieByImdbId(String imdbId) {
        Movie movie = movieRepository.findByImdbId(imdbId).orElse(null);

        if (movie == null) {
            movie = omdbService.getMovieFromOmdb(imdbId);
            if (movie == null) {
                throw new RuntimeException("Фильм не найден в OMDB API");
            }
            String genreText = movie.getGenreText() == null ? "" : movie.getGenreText();
            String firstGenreName = genreText.isBlank()
                    ? "Unknown"
                    : genreText.split(",")[0].trim();

            Genre genre = genreRepository.findByName(firstGenreName);
            if (genre == null) {
                genre = new Genre();
                genre.setName(firstGenreName);
                genre = genreRepository.save(genre);
            }
            movie.setGenre(genre);
            movie = movieRepository.save(movie);
        }

        return movie;
    }

    private MovieDto toWatchDto(Movie movie) {
        int year = movie.getYear() == null ? 0 : movie.getYear().intValue();
        Long genreId = movie.getGenre() != null ? movie.getGenre().getId() : null;

        MovieDto dto = new MovieDto(
                movie.getTitle(),
                year,
                movie.getImdbId(),
                genreId
        );

        dto.setStreamUrl(resolveStreamUrl(movie.getImdbId()));

        return dto;
    }

    // достаём из movie_streams относительный путь и собираем публичный URL
    private String resolveStreamUrl(String imdbId) {
        return movieStreamService.findEntityByImdbId(imdbId)
                .map(stream -> "/hls/" + stream.getStreamPath())
                .orElse(null);
    }
}
