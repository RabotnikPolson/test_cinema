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

        // We update the original subtitle status since the python worker updates the DB state
        // In theory it might be better to create a new MovieSubtitle for the new language, 
        // but as per our architecture design we update the original row's status fields to track.
        // Wait, does the movie have 1 subtitle row per language? Yes.
        // So we just find ANY subtitle row for that movie and update `translationStatus` or we query by movieId.
        
        // Let's just find the first one for the movie, or update all for the movie.
        List<MovieSubtitle> subtitles = subtitleRepository.findByMovieId(request.getMovieId());
        
        if (subtitles.isEmpty()) {
            log.error("MovieSubtitle not found for movie ID {}", request.getMovieId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Subtitle record not found");
        }

        // Just update the first one since it launched the process
        MovieSubtitle sub = subtitles.get(0);
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
