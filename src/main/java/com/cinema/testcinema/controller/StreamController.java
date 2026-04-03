package com.cinema.testcinema.controller;

import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.MovieRepository;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/stream")
public class StreamController {

    private final MovieRepository movieRepository;

    public StreamController(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @GetMapping("/{id}")
    @PermitAll
    public Map<String, String> stream(@PathVariable Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм с ID " + id + " не найден"));

        // Единое локальное хранилище согласно Фазе 3/4
        return Map.of(
                "type", "local_hls",
                "url", "/storage/movies/" + movie.getId() + "/master.m3u8"
        );
    }
}
