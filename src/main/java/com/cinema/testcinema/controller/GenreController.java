package com.cinema.testcinema.controller;

import com.cinema.testcinema.model.Genre;
import com.cinema.testcinema.repository.GenreRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/genres")
public class GenreController {

    private final GenreRepository genreRepository;

    public GenreController(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    /**
     * Получить все жанры.
     */
    @GetMapping
    public List<Genre> getAllGenres() {
        return genreRepository.findAll();
    }

    /**
     * Получить жанр по ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Genre> getGenreById(@PathVariable Long id) {
        Optional<Genre> genre = genreRepository.findById(id);
        return genre.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Создать новый жанр.
     */
    @PostMapping
    public Genre createGenre(@RequestBody Genre genre) {
        return genreRepository.save(genre);
    }
}
