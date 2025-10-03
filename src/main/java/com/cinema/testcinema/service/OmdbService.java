package com.cinema.testcinema.service;

import com.cinema.testcinema.model.Movie;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class OmdbService {

    private final String apiKey = "aee5595e";
    private final String apiUrl = "http://www.omdbapi.com/";

    public Movie getMovieFromOmdb(String imdbId) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("apikey", apiKey)
                .queryParam("i", imdbId);

        RestTemplate restTemplate = new RestTemplate();

        // Получаем JSON как Map
        Map<String, Object> response = restTemplate.getForObject(uri.toUriString(), Map.class);

        if (response == null || response.get("Response").equals("False")) {
            return null; // фильм не найден
        }

        Movie movie = new Movie();
        movie.setTitle((String) response.get("Title"));

        // В поле Year может быть "2017–" для сериалов, берём первые 4 цифры
        String yearStr = (String) response.get("Year");
        if (yearStr != null && yearStr.length() >= 4) {
            movie.setYear(Long.parseLong(yearStr.substring(0, 4)));
        }

        movie.setDescription((String) response.getOrDefault("Plot", ""));
        movie.setPosterUrl((String) response.getOrDefault("Poster", ""));
        movie.setImdbId(imdbId);

        return movie;
    }
}
