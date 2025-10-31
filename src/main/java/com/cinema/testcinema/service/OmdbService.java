// src/main/java/com/cinema/testcinema/service/OmdbService.java
package com.cinema.testcinema.service;

import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.MovieRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OmdbService {

    @Value("${omdb.api.key}")
    private String apiKey;

    private final MovieRepository movieRepository;

    public OmdbService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public Movie getMovieFromOmdb(String imdbId) {
        try {
            // если уже есть в БД — вернуть
            Movie existing = movieRepository.findByImdbId(imdbId).orElse(null);
            if (existing != null) return existing;

            String url = "http://www.omdbapi.com/?i=" + imdbId + "&apikey=" + apiKey + "&plot=full&r=json";
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);
            if (response == null || response.isBlank()) return null;

            JSONObject json = new JSONObject(response);
            if (!"True".equalsIgnoreCase(json.optString("Response"))) return null;

            Movie movie = new Movie();
            movie.setImdbId(imdbId);
            movie.setTitle(json.optString("Title", "No title"));
            movie.setYear(parseYear(json.optString("Year", "0")));
            movie.setDescription(json.optString("Plot", ""));
            movie.setPosterUrl(json.optString("Poster", ""));
            movie.setDirector(json.optString("Director", ""));
            movie.setActors(json.optString("Actors", ""));
            movie.setGenreText(json.optString("Genre", ""));
            movie.setLanguage(json.optString("Language", ""));
            movie.setCountry(json.optString("Country", ""));
            movie.setImdbRating(json.optString("imdbRating", ""));
            movie.setRuntime(json.optString("Runtime", ""));
            movie.setReleased(json.optString("Released", ""));
            return movie;

        } catch (Exception e) {
            return null;
        }
    }

    private Long parseYear(String yearStr) {
        try {
            String digits = yearStr == null ? "" : yearStr.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? 0L : Long.parseLong(digits);
        } catch (Exception e) {
            return 0L;
        }
    }
}
