package com.cinema.testcinema.controller;

import com.cinema.testcinema.model.Genre;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.GenreRepository;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.service.OmdbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/movies")
public class MovieController {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private OmdbService omdbService;

    @PostMapping("/addFromImdb")
    public Movie addFromImdb(@RequestParam String imdbId) {
        Movie movie = omdbService.getMovieFromOmdb(imdbId);
        if (movie == null) {
            throw new RuntimeException("Фильм не найден в OMDb API");
        }

        // достаём первый жанр из genreText (как и раньше)
        String genreText = movie.getGenreText() != null ? movie.getGenreText() : "";
        String firstGenreName = "Unknown";

        if (!genreText.isEmpty()) {
            if (genreText.contains(",")) {
                firstGenreName = genreText.split(",")[0].trim();
            } else {
                firstGenreName = genreText.trim();
            }
        }

        // берём/создаём жанр
        Genre genre = genreRepository.findByName(firstGenreName);
        if (genre == null) {
            genre = new Genre();
            genre.setName(firstGenreName);
            genre = genreRepository.save(genre);
        }

        // вместо movie.setGenre(...) — добавляем в many-to-many набор
        movie.getGenres().add(genre);

        return movieRepository.save(movie);
    }

    @GetMapping
    public Iterable<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    @GetMapping("/{id}")
    public Movie getMovieById(@PathVariable Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Фильм с ID " + id + " не найден"));
    }
}
