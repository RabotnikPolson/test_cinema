package com.cinema.testcinema.controller;

import com.cinema.testcinema.model.Genre;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.GenreRepository;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.service.OmdbService;
import com.cinema.testcinema.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Autowired
    private MovieService movieService;

    @PostMapping("/addFromImdb")
    @PreAuthorize("hasRole('ADMIN')")
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
    @Operation(summary = "Получить список фильмов (массив) с опциональными фильтрами и пагинацией")
    public java.util.List<Movie> getAllMovies(
            @Parameter(description = "Строка поиска по названию фильма (частичное совпадение, регистронезависимое)")
            @RequestParam(value = "q", required = false) String q,
            @Parameter(description = "ID жанра для фильтрации")
            @RequestParam(value = "genreId", required = false) Long genreId,
            @Parameter(description = "Нижняя граница года выпуска (включительно)")
            @RequestParam(value = "yearFrom", required = false) Long yearFrom,
            @Parameter(description = "Верхняя граница года выпуска (включительно)")
            @RequestParam(value = "yearTo", required = false) Long yearTo,
            @Parameter(description = "Номер страницы (0‑based)")
            @RequestParam(value = "page", required = false) Integer page,
            @Parameter(description = "Размер страницы")
            @RequestParam(value = "size", required = false) Integer size
    ) {
        return movieService.searchMovies(q, genreId, yearFrom, yearTo, page, size);
    }

    @GetMapping("/{id}")
    public Movie getMovieById(@PathVariable Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Фильм с ID " + id + " не найден"));
    }
}
