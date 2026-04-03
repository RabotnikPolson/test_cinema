package com.cinema.testcinema.controller;

import com.cinema.testcinema.service.SubtitleSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/subtitles")
@RequiredArgsConstructor
public class TestSubtitleController {

    private final SubtitleSyncService subtitleSyncService;

    @PostMapping("/discover")
    public String triggerDiscovery(@RequestParam Long movieId, @RequestParam String imdbId) {
        // Мы вызываем метод, который лезет в API и стейджит скачивание сабов в БД.
        subtitleSyncService.discoverAndStageSubtitles(movieId, imdbId);
        return "Discovery triggered for movie " + movieId + ". Check logs and DB!";
    }
}
