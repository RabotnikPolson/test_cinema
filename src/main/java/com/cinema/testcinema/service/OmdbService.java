package com.cinema.testcinema.service;

import com.cinema.testcinema.model.Movie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class OmdbService {

    private final String apiKey = "aee5595e";
    private final String apiUrl = "http://www.omdbapi.com/";
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Получить данные фильма из OMDB по IMDB ID.
     * Добавлен retry (до 2 попыток) на случай сетевых сбоев.
     */
    public Movie getMovieFromOmdb(String imdbId) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                        .queryParam("apikey", apiKey)
                        .queryParam("i", imdbId);

                ResponseEntity<Map> responseEntity = restTemplate.getForEntity(uri.toUriString(), Map.class);
                Map<String, Object> response = responseEntity.getBody();

                if (response == null || "False".equals(response.get("Response"))) {
                    return null; // Фильм не найден
                }

                Movie movie = new Movie();
                movie.setTitle(getStringOrDefault(response, "Title", ""));
                movie.setImdbId(imdbId);

                // Обработка года (может быть "2017–" для сериалов)
                String yearStr = getStringOrDefault(response, "Year", "0");
                if (yearStr.matches("\\d{4}.*")) { // Только если начинается с 4 цифр
                    movie.setYear(Long.parseLong(yearStr.substring(0, 4)));
                }

                movie.setDescription(getStringOrDefault(response, "Plot", ""));
                movie.setPosterUrl(getStringOrDefault(response, "Poster", ""));

                return movie;
            } catch (Exception e) {
                if (attempt == 1) throw new RuntimeException("Failed to fetch from OMDB: " + e.getMessage());
            }
        }
        return null;
    }

    private String getStringOrDefault(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.getOrDefault(key, defaultValue);
        return value != null && !"N/A".equals(value) ? value.toString() : defaultValue;
    }
}