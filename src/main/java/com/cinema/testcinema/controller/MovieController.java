package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.MovieDto;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.service.MovieService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @PostMapping
    public Movie addMovie(@RequestBody MovieDto movieDto) {
        return movieService.addMovie(movieDto);
    }
}
