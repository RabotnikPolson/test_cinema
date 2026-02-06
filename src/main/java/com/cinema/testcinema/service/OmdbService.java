package com.cinema.testcinema.service;

import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.MovieRepository;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.SocketTimeoutException;
import java.util.regex.Pattern;

@Service
public class OmdbService {

    private static final Pattern IMDB_ID_PATTERN = Pattern.compile("^tt\\d{7,8}$");

    @Value("${omdb.api.key}")
    private String apiKey;

    private final MovieRepository movieRepository;

    public OmdbService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public Movie getMovieFromOmdb(String imdbId) {
        try {
            JSONObject json = requestOmdbJson(imdbId);

            if (!json.optBoolean("Response", false)) {
                return null;
            }

            // Проверяем, есть ли фильм уже в БД
            Movie existing = movieRepository.findByImdbId(imdbId);
            if (existing != null) return existing;

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
            movie.setImdbVotes(json.optString("imdbVotes", ""));

            // 🔥 Парсим массив Ratings
            JSONArray ratingsArray = json.optJSONArray("Ratings");
            if (ratingsArray != null) {
                for (int i = 0; i < ratingsArray.length(); i++) {
                    JSONObject ratingObj = ratingsArray.getJSONObject(i);
                    String source = ratingObj.optString("Source", "");
                    String value = ratingObj.optString("Value", "");

                    if (source.equalsIgnoreCase("Rotten Tomatoes")) {
                        movie.setRottenTomatoesRating(value);
                    } else if (source.equalsIgnoreCase("Metacritic")) {
                        movie.setMetacriticRating(value);
                    }
                }
            }

            return movie;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Movie fetchMovieOrThrow(String imdbId) {
        if (imdbId == null || !IMDB_ID_PATTERN.matcher(imdbId).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный imdbId. Ожидается формат tt1234567 или tt12345678");
        }

        Movie existing = movieRepository.findByImdbId(imdbId);
        if (existing != null) {
            return existing;
        }

        JSONObject json;
        try {
            json = requestOmdbJson(imdbId);
        } catch (ResourceAccessException ex) {
            if (isTimeout(ex)) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OMDb timeout/unavailable", ex);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OMDb network error", ex);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OMDb gateway error", ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OMDb response parse error", ex);
        }

        if (!json.optBoolean("Response", false)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден в OMDb API");
        }

        return mapMovieFromJson(imdbId, json);
    }

    private JSONObject requestOmdbJson(String imdbId) {
        String url = "http://www.omdbapi.com/?i=" + imdbId + "&apikey=" + apiKey;
        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.getForObject(url, String.class);
        return new JSONObject(response);
    }

    private Movie mapMovieFromJson(String imdbId, JSONObject json) {
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
        movie.setImdbVotes(json.optString("imdbVotes", ""));

        JSONArray ratingsArray = json.optJSONArray("Ratings");
        if (ratingsArray != null) {
            for (int i = 0; i < ratingsArray.length(); i++) {
                JSONObject ratingObj = ratingsArray.getJSONObject(i);
                String source = ratingObj.optString("Source", "");
                String value = ratingObj.optString("Value", "");

                if (source.equalsIgnoreCase("Rotten Tomatoes")) {
                    movie.setRottenTomatoesRating(value);
                } else if (source.equalsIgnoreCase("Metacritic")) {
                    movie.setMetacriticRating(value);
                }
            }
        }
        return movie;
    }

    private boolean isTimeout(ResourceAccessException ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        String msg = ex.getMessage();
        return msg != null && msg.toLowerCase().contains("timed out");
    }

    private Long parseYear(String yearStr) {
        try {
            return Long.parseLong(yearStr.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0L;
        }
    }
}
