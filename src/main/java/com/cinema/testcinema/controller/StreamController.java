package com.cinema.testcinema.controller;

import jakarta.annotation.security.PermitAll;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/stream")
public class StreamController {

    @GetMapping("/{id}")
    @PermitAll
    public Map<String, String> stream(@PathVariable Long id) {
        return Map.of(
                "type", "hls",
                "url", "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
        );
    }
}

