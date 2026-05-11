package com.cinema.testcinema.controller;

import com.cinema.testcinema.service.StreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/stream")
@Tag(name = "Stream Controller", description = "Эндпоинты для стриминга видео и субтитров")
public class StreamController {

    private final StreamService streamService;

    public StreamController(StreamService streamService) {
        this.streamService = streamService;
    }

    @GetMapping("/{id}")
    @PermitAll
    @Operation(summary = "Получить ссылки на видео и субтитры", description = "Генерирует временные presigned-ссылки на MinIO")
    public ResponseEntity<Map<String, String>> stream(@PathVariable Long id) {
        return ResponseEntity.ok(streamService.getStreamUrls(id));
    }
}
