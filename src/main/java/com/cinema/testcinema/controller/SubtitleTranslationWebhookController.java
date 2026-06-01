package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.SubtitleWebhookRequest;
import com.cinema.testcinema.model.MovieSubtitle;
import com.cinema.testcinema.repository.MovieSubtitleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/internal/subtitles")
public class SubtitleTranslationWebhookController {

    private static final Logger log = LoggerFactory.getLogger(SubtitleTranslationWebhookController.class);

    private final MovieSubtitleRepository subtitleRepository;

    @Value("${ai.translator.secret:change-me-in-prod}")
    private String internalSecret;

    public SubtitleTranslationWebhookController(MovieSubtitleRepository subtitleRepository) {
        this.subtitleRepository = subtitleRepository;
    }

    @PostMapping("/translation-complete")
    public ResponseEntity<String> handleTranslationWebhook(
            @RequestHeader(value = "X-Internal-Secret", required = false) String secret,
            @RequestBody SubtitleWebhookRequest request) {

        if (secret == null || !secret.equals(internalSecret)) {
            log.warn("Received webhook with invalid or missing secret");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid secret");
        }

        log.info("Received translation webhook for movie ID: {}, status: {}", request.getMovieId(), request.getStatus());

        // Find the subtitle record that is actively being translated (pending or in_progress).
        // A movie can have multiple subtitle rows (ru, en, kk) — taking get(0) would update
        // the wrong record if the first discovered language differs from the one being translated.
        Optional<MovieSubtitle> activeSubtitle = subtitleRepository
                .findFirstByMovieIdAndTranslationStatusIn(
                        request.getMovieId(),
                        List.of("pending", "in_progress")
                );

        MovieSubtitle sub;
        if (activeSubtitle.isPresent()) {
            sub = activeSubtitle.get();
        } else {
            // Fallback: webhook may have arrived after a status update (e.g. retry). Take the first downloaded row.
            List<MovieSubtitle> all = subtitleRepository.findByMovieId(request.getMovieId());
            if (all.isEmpty()) {
                log.error("MovieSubtitle not found for movie ID {}", request.getMovieId());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Subtitle record not found");
            }
            log.warn("No pending/in_progress subtitle for movie {}, falling back to first record (id={})",
                    request.getMovieId(), all.get(0).getId());
            sub = all.get(0);
        }
        sub.setTranslationStatus(request.getStatus());
        
        if (request.getOutputPath() != null) {
            sub.setTranslatedPath(request.getOutputPath());
        }
        if (request.getLinesTranslated() != null) {
            sub.setLinesTranslated(request.getLinesTranslated());
        }

        subtitleRepository.save(sub);

        if ("failed".equals(request.getStatus())) {
            log.error("Translation for movie {} failed: {}", request.getMovieId(), request.getErrorMessage());
        } else {
            log.info("Movie {} translation updated successfully. Path: {}", request.getMovieId(), request.getOutputPath());
        }

        return ResponseEntity.ok("Webhook processed");
    }
}
