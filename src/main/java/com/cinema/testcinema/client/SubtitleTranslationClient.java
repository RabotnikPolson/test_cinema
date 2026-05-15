package com.cinema.testcinema.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Component
public class SubtitleTranslationClient {

    private static final Logger log = LoggerFactory.getLogger(SubtitleTranslationClient.class);

    @Value("${ai.translator.url:http://localhost:8100/api/translate}")
    private String translatorUrl;

    private final RestTemplate restTemplate;

    public SubtitleTranslationClient() {
        this.restTemplate = new RestTemplate();
    }

    public void triggerTranslation(Long movieId, String inputPath, String outputPath, String movieTitle, String sourceLanguage) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("movie_id", movieId);
            body.put("input_path", inputPath);
            body.put("output_path", outputPath);
            body.put("movie_title", movieTitle);
            body.put("source_language", sourceLanguage);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            log.info("Sending translation request to subtitle-translator for movie ID {}", movieId);
            ResponseEntity<String> response = restTemplate.postForEntity(translatorUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully queued translation for movie ID {}, task info: {}", movieId, response.getBody());
            } else {
                log.warn("Non-2xx response from translation service for movie ID {}: {}", movieId, response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Failed to trigger subtitle translation microservice for movie ID {}: {}", movieId, e.getMessage());
        }
    }
}
