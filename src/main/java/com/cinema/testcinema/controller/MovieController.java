// src/main/java/com/cinema/testcinema/controller/MovieController.java
package com.cinema.testcinema.controller;

import com.cinema.testcinema.model.Genre;
import com.cinema.testcinema.model.Movie;
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

    public MovieController(MovieRepository movieRepository,
                           GenreRepository genreRepository,
                           OmdbService omdbService) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.omdbService = omdbService;
    }

    // отдельный путь, чтобы не конфликтовать с /{id}
    @GetMapping("/imdb/{imdbId}")
    public Movie getMovieByImdbId(@PathVariable String imdbId) {
        Movie movie = movieRepository.findByImdbId(imdbId).orElse(null);

        if (movie == null) {
            movie = omdbService.getMovieFromOmdb(imdbId);
            if (movie == null) {
                throw new RuntimeException("Фильм не найден в OMDB API");
            }
            // жанр
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
}
