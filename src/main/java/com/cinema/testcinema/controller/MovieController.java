package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.MovieDto;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.service.MovieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping("/movies")
    public List<Movie> getAllMovies() {
        return movieService.getAllMovies();
    }

    @GetMapping("/series")
    public List<Movie> getAllSeries() {
        return movieService.getAllSeries();
    }

    @GetMapping("/movies/{id}")
    public ResponseEntity<Movie> getMovieById(@PathVariable Long id) {
        return movieService.getMovieById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/movies")
    public ResponseEntity<Movie> addMovie(@RequestBody MovieDto movieDto) {
        try {
            Movie movie = movieService.addMovie(movieDto);
            return ResponseEntity.ok(movie);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
