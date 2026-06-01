package com.cinema.testcinema.controller;

import com.cinema.testcinema.client.SubtitleTranslationClient;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.MovieSubtitle;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.MovieSubtitleRepository;
import com.cinema.testcinema.service.SubtitleDownloadWorker;
import com.cinema.testcinema.service.SubtitleMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test/subtitles")
@Tag(name = "Test Subtitles", description = "Эндпоинты для ручного тестирования загрузки субтитров")
public class SubtitleTestController {

    private final SubtitleMetadataService metadataService;
    private final SubtitleDownloadWorker downloadWorker;
    private final MovieRepository movieRepository;
    private final MovieSubtitleRepository subtitleRepository;
    private final SubtitleTranslationClient translationClient;

    public SubtitleTestController(SubtitleMetadataService metadataService,
                                  SubtitleDownloadWorker downloadWorker,
                                  MovieRepository movieRepository,
                                  MovieSubtitleRepository subtitleRepository,
                                  SubtitleTranslationClient translationClient) {
        this.metadataService = metadataService;
        this.downloadWorker = downloadWorker;
        this.movieRepository = movieRepository;
        this.subtitleRepository = subtitleRepository;
        this.translationClient = translationClient;
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

    @DeleteMapping("/{id}/translate")
    @Operation(summary = "Отменить перевод субтитра", description = "Останавливает активный перевод между чанками")
    public ResponseEntity<String> cancelTranslate(@PathVariable Long id) {
        MovieSubtitle subtitle = subtitleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Субтитр не найден: " + id));

        translationClient.cancelTranslation(subtitle.getMovie().getId());

        subtitle.setTranslationStatus("pending");
        subtitleRepository.save(subtitle);

        return ResponseEntity.ok("Перевод отменён для: " + subtitle.getMovie().getTitle());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить запись субтитра", description = "Удаляет строку из movie_subtitles по ID")
    public ResponseEntity<String> deleteSubtitle(@PathVariable Long id) {
        MovieSubtitle subtitle = subtitleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Субтитр не найден: " + id));
        subtitleRepository.delete(subtitle);
        return ResponseEntity.ok("Удалено");
    }

    @PostMapping("/{id}/translate")
    @Operation(summary = "Запустить перевод конкретного субтитра", description = "Отправляет уже скачанный файл в subtitle-translator")
    public ResponseEntity<String> triggerTranslate(@PathVariable Long id) {
        MovieSubtitle subtitle = subtitleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Субтитр не найден: " + id));

        if (!subtitle.isDownloaded() || subtitle.getLocalPath() == null) {
            return ResponseEntity.badRequest().body("Файл ещё не скачан");
        }

        String absoluteInputPath = java.nio.file.Path.of(subtitle.getLocalPath())
                .toAbsolutePath().normalize().toString().replace('\\', '/');
        String absoluteOutputPath = absoluteInputPath
                .replace(subtitle.getLanguage() + ".vtt", "kk.srt")
                .replace(subtitle.getLanguage() + ".srt", "kk.srt");

        subtitle.setTranslationStatus("in_progress");
        subtitleRepository.save(subtitle);

        translationClient.triggerTranslation(
                subtitle.getMovie().getId(),
                absoluteInputPath,
                absoluteOutputPath,
                subtitle.getMovie().getTitle(),
                subtitle.getLanguage()
        );

        return ResponseEntity.ok("Перевод запущен для: " + subtitle.getMovie().getTitle());
    }
}