package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.movie.TrendingMovieDto;
import com.cinema.testcinema.service.CollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/collections")
@Tag(name = "Collections", description = "Подборки фильмов для главной страницы")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @GetMapping("/new-releases")
    @Operation(summary = "Новинки кино", description = "Кэшируется на 30 минут. Возвращает DTO.")
    public List<TrendingMovieDto> getNewReleases() {
        return collectionService.getNewReleases();
    }

    @GetMapping("/top-comedies")
    @Operation(summary = "Топ комедий", description = "Кэшируется на 30 минут. Возвращает DTO.")
    public List<TrendingMovieDto> getTopComedies() {
        return collectionService.getTopComedies();
    }
}
