package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.MovieStreamDto;
import com.cinema.testcinema.service.MovieStreamService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/streams")
public class AdminStreamController {

    private final MovieStreamService movieStreamService;

    public AdminStreamController(MovieStreamService movieStreamService) {
        this.movieStreamService = movieStreamService;
    }

    // список всех стримов
    @GetMapping
    public List<MovieStreamDto> getAll() {
        return movieStreamService.findAll();
    }

    // получить стрим по imdbId
    @GetMapping("/{imdbId}")
    public MovieStreamDto getByImdbId(@PathVariable String imdbId) {
        return movieStreamService.findByImdbId(imdbId)
                .orElseThrow(() -> new RuntimeException("Stream not found for imdbId: " + imdbId));
    }

    // создать или обновить стрим
    @PostMapping
    public MovieStreamDto createOrUpdate(@RequestBody MovieStreamDto dto) {
        return movieStreamService.createOrUpdate(dto);
    }

    // удалить стрим
    @DeleteMapping("/{imdbId}")
    public void delete(@PathVariable String imdbId) {
        movieStreamService.deleteByImdbId(imdbId);
    }
}
