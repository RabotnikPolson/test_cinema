package com.cinema.testcinema.service;

import com.cinema.testcinema.model.Genre;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.GenreRepository;
import com.cinema.testcinema.repository.MovieRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * Синхронизирует метаданные из Kinopoisk Unofficial API в локальную БД.
 * <p>
 * Используется:
 * 1. При добавлении фильма по kinopoiskId (ADMIN endpoint).
 * 2. При обогащении уже существующих фильмов данными КП.
 */
@Service
public class KinopoiskSyncService {

    private final KinopoiskClient client;
    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;

    public KinopoiskSyncService(KinopoiskClient client,
                                 MovieRepository movieRepository,
                                 GenreRepository genreRepository) {
        this.client = client;
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
    }

    /**
     * Получает данные фильма из КП API и сохраняет в БД.
     * Если фильм уже существует (по kinopoiskId) — обновляет его данные.
     *
     * @param kinopoiskId числовой ID фильма в Кинопоиске
     * @return сохранённая сущность Movie
     */
    @Transactional
    public Movie fetchAndSave(String kinopoiskId) {
        JsonNode data = client.fetchFilm(kinopoiskId);

        // Проверяем: фильм уже есть?
        Movie movie = movieRepository.findByKinopoiskId(kinopoiskId)
                .orElse(new Movie());

        // Основные поля
        movie.setKinopoiskId(kinopoiskId);
        movie.setTitle(getTextOrDefault(data, "nameRu", getTextOrDefault(data, "nameEn", "Без названия")));
        movie.setDescription(getTextOrDefault(data, "description", null));
        movie.setPosterUrl(getTextOrDefault(data, "posterUrl", null));
        movie.setImdbRating(getTextOrDefault(data, "ratingImdb", null));

        // Год
        if (data.hasNonNull("year")) {
            movie.setYear(data.get("year").asLong());
        }

        // imdbId (если есть в ответе)
        String imdbId = getTextOrDefault(data, "imdbId", null);
        if (imdbId != null && movie.getImdbId() == null) {
            movie.setImdbId(imdbId);
        }

        // Страна (берём первую из массива)
        if (data.hasNonNull("countries") && data.get("countries").isArray()
                && data.get("countries").size() > 0) {
            movie.setCountry(data.get("countries").get(0).path("country").asText(null));
        }

        // Жанры — парсим массив и синхронизируем с таблицей genres
        if (data.hasNonNull("genres") && data.get("genres").isArray()) {
            StringBuilder genreText = new StringBuilder();
            for (JsonNode g : data.get("genres")) {
                String genreName = g.path("genre").asText("");
                if (genreName.isBlank()) continue;

                // Заглавная первая буква для единообразия
                genreName = capitalize(genreName);
                if (!genreText.isEmpty()) genreText.append(", ");
                genreText.append(genreName);

                // Убеждаемся что жанр есть в справочнике
                Genre genre = genreRepository.findByName(genreName);
                if (genre == null) {
                    genre = genreRepository.save(new Genre(genreName));
                }
                movie.getGenres().add(genre);
            }
            movie.setGenreText(genreText.toString());
        }

        return movieRepository.save(movie);
    }

    // ─── Утилиты ─────────────────────────────────────────────────────────────

    private String getTextOrDefault(JsonNode node, String field, String defaultVal) {
        if (node.hasNonNull(field)) {
            String val = node.get(field).asText("").trim();
            return val.isEmpty() ? defaultVal : val;
        }
        return defaultVal;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
