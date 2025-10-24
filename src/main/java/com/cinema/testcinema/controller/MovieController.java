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

    /**
     * Добавление фильма из OMDb по IMDb ID.
     * Пример: /movies/addFromImdb?imdbId=tt0468569
     */
    @PostMapping("/addFromImdb")
    public Movie addFromImdb(@RequestParam String imdbId) {
        Movie movie = omdbService.getMovieFromOmdb(imdbId);
        if (movie == null) {
            throw new RuntimeException("Фильм не найден в OMDb API");
        }

        // Достаём первый жанр из поля genreText
        String genreText = movie.getGenreText() != null ? movie.getGenreText() : "";
        String firstGenreName = "Unknown";

        if (!genreText.isEmpty()) {
            if (genreText.contains(",")) {
                firstGenreName = genreText.split(",")[0].trim();
            } else {
                firstGenreName = genreText.trim();
            }
        }

        // Проверяем, есть ли уже жанр с таким именем
        Genre genre = genreRepository.findByName(firstGenreName);
        if (genre == null) {
            genre = new Genre();
            genre.setName(firstGenreName); // ✅ ВАЖНО: тут именно название жанра, не описание
            genre = genreRepository.save(genre);
        }

        movie.setGenre(genre);
        return movieRepository.save(movie);
    }

    /**
     * Получить все фильмы
     */
    @GetMapping
    public Iterable<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    /**
     * Получить фильм по ID
     */
    @GetMapping("/{id}")
    public Movie getMovieById(@PathVariable Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Фильм с ID " + id + " не найден"));
    }
}
