package com.cinema.testcinema.service;

import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;

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
            String url = "http://www.omdbapi.com/?i=" + imdbId + "&apikey=" + apiKey;
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);

            JSONObject json = new JSONObject(response);

            if (!json.getBoolean("Response")) {
                return null;
            }

            // Проверяем, есть ли фильм уже в БД
            Movie existing = movieRepository.findByImdbId(imdbId);
            if (existing != null) return existing;

            // Создаём новый объект фильма
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
            e.printStackTrace();
            return null;
        }
    }

    private Long parseYear(String yearStr) {
        try {
            return Long.parseLong(yearStr.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0L;
        }
    }
}
