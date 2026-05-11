package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.movie.TrendingMovieDto;
import com.cinema.testcinema.service.TrendingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/trending")
@Tag(name = "Trending Controller", description = "Лента трендов фильмов")
public class TrendingController {

    private final TrendingService trendingService;

    public TrendingController(TrendingService trendingService) {
        this.trendingService = trendingService;
    }

    @GetMapping
    @PermitAll
    @Operation(summary = "Получить список трендовых фильмов", description = "Агрегирует Топ-20 Кинопоиска и внутренние клики")
    public ResponseEntity<List<TrendingMovieDto>> getTrending() {
        return ResponseEntity.ok(trendingService.getTrendingMovies());
    }
}
