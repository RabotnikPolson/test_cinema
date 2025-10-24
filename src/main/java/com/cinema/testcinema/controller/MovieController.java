package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.MovieDto;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.service.MovieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies") // базовый путь для всех эндпоинтов фильмов
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    /**
     * Получить все фильмы по типу.
     * Пример: GET /api/movies/type/movie
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Movie>> getAllMoviesByType(@PathVariable String type) {
        List<Movie> movies = movieService.getAllMoviesByType(type);
        return ResponseEntity.ok(movies);
    }

    /**
     * Получить фильм по ID.
     * Пример: GET /api/movies/5
     */
    @GetMapping("/{id}")
    public ResponseEntity<Movie> getMovieById(@PathVariable Long id) {
        return movieService.getMovieById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Добавить новый фильм.
     * Пример: POST /api/movies
     */
    @PostMapping
    public ResponseEntity<Movie> addMovie(@RequestBody MovieDto movieDto) {
        try {
            Movie movie = movieService.addMovie(movieDto);
            return ResponseEntity.ok(movie);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}
