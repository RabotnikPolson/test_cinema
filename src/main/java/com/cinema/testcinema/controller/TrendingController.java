package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.movie.TrendingMovieDto;
import com.cinema.testcinema.service.TrendingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import com.cinema.testcinema.service.TrendingRedisService;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.model.Movie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Collections;

@RestController
@RequestMapping("/trending")
@Tag(name = "Trending Controller", description = "Лента трендов фильмов")
public class TrendingController {

    private final TrendingService trendingService;
    private final TrendingRedisService trendingRedisService;
    private final MovieRepository movieRepository;

    public TrendingController(TrendingService trendingService, 
                              TrendingRedisService trendingRedisService,
                              MovieRepository movieRepository) {
        this.trendingService = trendingService;
        this.trendingRedisService = trendingRedisService;
        this.movieRepository = movieRepository;
    }

    @GetMapping
    @PermitAll
    @Operation(summary = "Получить список трендовых фильмов", description = "Агрегирует Топ-20 Кинопоиска и внутренние клики")
    public ResponseEntity<List<TrendingMovieDto>> getTrending() {
        return ResponseEntity.ok(trendingService.getTrendingMovies());
    }

    @GetMapping("/weekly")
    @PermitAll
    @Operation(summary = "Получить еженедельный ТОП фильмов (Redis ZSET)", description = "Достает ТОП-10 фильмов на основе кликов пользователей")
    public ResponseEntity<List<TrendingMovieDto>> getWeeklyTrending() {
        List<Long> topIds = trendingRedisService.getTopTrendingIds(10);
        if (topIds.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        Map<Long, Movie> movieById = movieRepository.findAllById(topIds).stream()
                .collect(Collectors.toMap(Movie::getId, m -> m));

        List<TrendingMovieDto> sortedDtos = topIds.stream()
                .filter(movieById::containsKey)
                .map(id -> {
                    Movie m = movieById.get(id);
                    return new TrendingMovieDto(m.getId(), m.getTitle(), m.getPosterUrl(), 0.0, m.isDomestic());
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(sortedDtos);
    }
}
