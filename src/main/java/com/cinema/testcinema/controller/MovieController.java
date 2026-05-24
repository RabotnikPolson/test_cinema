package com.cinema.testcinema.controller;

import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.service.KinopoiskSyncService;
import com.cinema.testcinema.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import com.cinema.testcinema.dto.movie.BulkImportRequest;
import com.cinema.testcinema.dto.movie.BulkImportResponse;
import com.cinema.testcinema.service.BulkImportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/movies")
public class MovieController {

    private final MovieRepository movieRepository;
    private final KinopoiskSyncService kinopoiskSyncService;
    private final MovieService movieService;
    private final BulkImportService bulkImportService;

    public MovieController(MovieRepository movieRepository,
                           KinopoiskSyncService kinopoiskSyncService,
                           MovieService movieService,
                           BulkImportService bulkImportService) {
        this.movieRepository = movieRepository;
        this.kinopoiskSyncService = kinopoiskSyncService;
        this.movieService = movieService;
        this.bulkImportService = bulkImportService;
    }

    /**
     * Добавить фильм по Kinopoisk ID. Метаданные подтягиваются из API КП автоматически.
     * Пример: POST /movies/addFromKinopoisk?kinopoiskId=301 (Матрица)
     */
    @PostMapping("/addFromKinopoisk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Добавить/обновить фильм по ID Кинопоиска (ADMIN)")
    @Caching(evict = {
        @CacheEvict(value = "movie_detail", key = "#result.id"),
        @CacheEvict(value = "home_collections", allEntries = true)
    })
    public Movie addFromKinopoisk(
            @Parameter(description = "Числовой ID фильма на kinopoisk.ru (например, 301 – это 'Матрица')")
            @RequestParam String kinopoiskId) {
        if (kinopoiskId == null || kinopoiskId.isBlank()) {
            throw new IllegalArgumentException("kinopoiskId не может быть пустым");
        }
        return kinopoiskSyncService.fetchAndSave(kinopoiskId);
    }

    @PostMapping("/bulkImport")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Массовый импорт фильмов по списку Kinopoisk ID (ADMIN)")
    @CacheEvict(value = "home_collections", allEntries = true)
    public BulkImportResponse bulkImport(@Valid @RequestBody BulkImportRequest request) {
        return bulkImportService.bulkImport(request.kinopoiskIds());
    }

    @GetMapping
    @Operation(summary = "Получить список фильмов с опциональными фильтрами и пагинацией")
    public List<Movie> getAllMovies(
            @Parameter(description = "Строка поиска по названию (частичное совпадение)")
            @RequestParam(value = "q", required = false) String q,
            @Parameter(description = "ID жанра для фильтрации")
            @RequestParam(value = "genreId", required = false) Long genreId,
            @Parameter(description = "Нижняя граница года выпуска (включительно)")
            @RequestParam(value = "yearFrom", required = false) Long yearFrom,
            @Parameter(description = "Верхняя граница года выпуска (включительно)")
            @RequestParam(value = "yearTo", required = false) Long yearTo,
            @Parameter(description = "Номер страницы (0-based)")
            @RequestParam(value = "page", required = false) Integer page,
            @Parameter(description = "Размер страницы")
            @RequestParam(value = "size", required = false) Integer size
    ) {
        return movieService.searchMovies(q, genreId, yearFrom, yearTo, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить фильм по внутреннему ID")
    public Movie getMovieById(@PathVariable Long id) {
        return movieService.getMovieById(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить фильм по ID (ADMIN)")
    @Caching(evict = {
        @CacheEvict(value = "movie_detail", key = "#id"),
        @CacheEvict(value = "home_collections", allEntries = true)
    })
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        if (!movieRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм с ID " + id + " не найден");
        }
        movieRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
