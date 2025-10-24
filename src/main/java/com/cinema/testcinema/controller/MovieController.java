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

    // Добавление фильма из OMDb
    @PostMapping("/addFromImdb")
    public Movie addFromImdb(@RequestParam String imdbId) {
        Movie movie = omdbService.getMovieFromOmdb(imdbId);
        if (movie == null) {
            throw new RuntimeException("Фильм не найден в OMDb API");
        }

        // Берём первый жанр из OMDb
        String genreText = movie.getGenreText() != null ? movie.getGenreText() : "";
        String firstGenreName = "Unknown";

        if (genreText.contains(",")) {
            firstGenreName = genreText.split(",")[0].trim();
        } else if (!genreText.isEmpty()) {
            firstGenreName = genreText.trim();
        }

        // Проверяем жанр в БД
        Genre genre = genreRepository.findByName(firstGenreName);
        if (genre == null) {
            genre = new Genre(firstGenreName);
            genre = genreRepository.save(genre);
        }

        movie.setGenre(genre);

        return movieRepository.save(movie);
    }
}
