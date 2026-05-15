package com.cinema.testcinema.controller;

import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.service.SubtitleDownloadWorker;
import com.cinema.testcinema.service.SubtitleMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/subtitles")
@Tag(name = "Test Subtitles", description = "Эндпоинты для ручного тестирования загрузки субтитров")
public class SubtitleTestController {

    private final SubtitleMetadataService metadataService;
    private final SubtitleDownloadWorker downloadWorker;
    private final MovieRepository movieRepository;

    public SubtitleTestController(SubtitleMetadataService metadataService, 
                                  SubtitleDownloadWorker downloadWorker, 
                                  MovieRepository movieRepository) {
        this.metadataService = metadataService;
        this.downloadWorker = downloadWorker;
        this.movieRepository = movieRepository;
    }

    @PostMapping("/discover/{movieId}")
    @Operation(summary = "1. Инициировать поиск", description = "Ищет метаданные субтитров для фильма и ставит в очередь (БД)")
    public ResponseEntity<String> forceDiscover(@PathVariable Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Фильм не найден"));
        
        metadataService.discoverForMovie(movie);
        return ResponseEntity.ok("Поиск завершен. Проверь логи и БД (таблицу movie_subtitles).");
    }

    @PostMapping("/trigger-worker")
    @Operation(summary = "2. Разбудить Воркер", description = "Принудительно запускает фоновый процесс скачивания (без ожидания 30 минут)")
    public ResponseEntity<String> triggerWorker() {
        downloadWorker.processDownloadQueue();
        return ResponseEntity.ok("Воркер отработал. Проверь логи, БД (is_downloaded=true) и папку ./storage/subtitles/");
    }
}
