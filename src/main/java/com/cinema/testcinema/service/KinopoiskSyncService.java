package com.cinema.testcinema.service;

import com.cinema.testcinema.model.Genre;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.GenreRepository;
import com.cinema.testcinema.repository.MovieRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Синхронизирует метаданные из Kinopoisk Unofficial API в локальную БД.
 * Выполняет два запроса:
 *  1. GET /api/v2.2/films/{id}         — основные данные фильма
 *  2. GET /api/v1/staff?filmId={id}    — режиссёр + топ-5 актёров
 */
@Service
public class KinopoiskSyncService {

    private static final Logger log = LoggerFactory.getLogger(KinopoiskSyncService.class);

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
     * Получает данные фильма из КП API и сохраняет/обновляет в БД.
     *
     * @param kinopoiskId числовой ID фильма в Кинопоиске
     * @return сохранённая сущность Movie
     */
    @Transactional
    public Movie fetchAndSave(String kinopoiskId) {
        // ── Запрос 1: Основные данные ─────────────────────────────────────
        JsonNode data = client.fetchFilm(kinopoiskId);

        Movie movie = movieRepository.findByKinopoiskId(kinopoiskId)
                .orElse(new Movie());

        movie.setKinopoiskId(kinopoiskId);

        // Название: приоритет RU → EN → Original
        String title = firstNonBlank(
                textOf(data, "nameRu"),
                textOf(data, "nameEn"),
                textOf(data, "nameOriginal"),
                "Без названия"
        );
        movie.setTitle(title);

        // Оригинальное название — сохраняем в отдельное поле (если оно отличается)
        // пока сохраним в actors как временный workaround до рефакторинга Movie
        String nameOriginal = textOf(data, "nameOriginal");

        // Описание
        movie.setDescription(textOf(data, "description"));

        // Постер
        movie.setPosterUrl(textOf(data, "posterUrl"));

        // Год
        if (data.hasNonNull("year")) {
            movie.setYear(data.get("year").asLong());
        }

        // imdbId
        String imdbId = textOf(data, "imdbId");
        if (imdbId != null && movie.getImdbId() == null) {
            movie.setImdbId(imdbId);
        }

        // Рейтинг КП → imdbRating (временно используем это поле, пока нет kpRating)
        // Формат: "7.9" (КП) или "7.5" (IMDB)
        if (data.hasNonNull("ratingKinopoisk")) {
            movie.setImdbRating(data.get("ratingKinopoisk").asText());
        } else if (data.hasNonNull("ratingImdb")) {
            movie.setImdbRating(data.get("ratingImdb").asText());
        }

        // Голосов на КП
        if (data.hasNonNull("ratingKinopoiskVoteCount")) {
            movie.setImdbVotes(data.get("ratingKinopoiskVoteCount").asText());
        }

        // Хронометраж (filmLength) — API возвращает число (минуты) или строку "2:17"
        if (data.hasNonNull("filmLength")) {
            JsonNode fl = data.get("filmLength");
            if (fl.isNumber()) {
                movie.setRuntime(fl.asInt() + " мин");
            } else {
                movie.setRuntime(fl.asText());
            }
        }

        // Слоган → released (временно, нет отдельного поля)
        String slogan = textOf(data, "slogan");
        if (slogan != null) {
            movie.setReleased(slogan);
        }

        // Тип (FILM / TV_SERIES / MINI_SERIES...)
        String type = textOf(data, "type");
        if (type != null) {
            movie.setLanguage(type); // временно пишем тип в language
        }

        // Страна — первая из массива
        if (data.hasNonNull("countries") && data.get("countries").isArray() && data.get("countries").size() > 0) {
            movie.setCountry(data.get("countries").get(0).path("country").asText(null));
        }

        // Жанры — синхронизируем с таблицей genres
        if (data.hasNonNull("genres") && data.get("genres").isArray()) {
            StringBuilder genreText = new StringBuilder();
            for (JsonNode g : data.get("genres")) {
                String genreName = g.path("genre").asText("").trim();
                if (genreName.isBlank()) continue;

                genreName = capitalize(genreName);
                if (!genreText.isEmpty()) genreText.append(", ");
                genreText.append(genreName);

                Genre genre = genreRepository.findByName(genreName);
                if (genre == null) {
                    genre = genreRepository.save(new Genre(genreName));
                }
                movie.getGenres().add(genre);
            }
            movie.setGenreText(genreText.toString());
        }

        // ── Запрос 2: Съёмочная группа (/api/v1/staff) ───────────────────
        try {
            JsonNode staffArray = client.fetchStaff(kinopoiskId);
            if (staffArray != null && staffArray.isArray()) {
                List<String> directors = new ArrayList<>();
                List<String> actors = new ArrayList<>();

                for (JsonNode person : staffArray) {
                    String profKey = person.path("professionKey").asText("");
                    String nameRu  = person.path("nameRu").asText("").trim();
                    if (nameRu.isBlank()) {
                        nameRu = person.path("nameEn").asText("").trim();
                    }
                    if (nameRu.isBlank()) continue;

                    if ("DIRECTOR".equalsIgnoreCase(profKey)) {
                        directors.add(nameRu);
                    } else if ("ACTOR".equalsIgnoreCase(profKey) && actors.size() < 7) {
                        actors.add(nameRu);
                    }
                }

                if (!directors.isEmpty()) {
                    movie.setDirector(String.join(", ", directors));
                }
                if (!actors.isEmpty()) {
                    movie.setActors(String.join(", ", actors));
                }
            }
        } catch (Exception e) {
            // Staff не критично — продолжаем без неё
            log.warn("Не удалось загрузить staff для kinopoiskId={}: {}", kinopoiskId, e.getMessage());
        }

        Movie saved = movieRepository.save(movie);
        log.info("Синхронизирован фильм: '{}' (kinopoiskId={})", saved.getTitle(), kinopoiskId);
        return saved;
    }

    // ── Утилиты ───────────────────────────────────────────────────────────────

    private String textOf(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) return null;
        String val = node.get(field).asText("").trim();
        return val.isEmpty() ? null : val;
    }

    private String firstNonBlank(String... candidates) {
        for (String s : candidates) {
            if (s != null && !s.isBlank()) return s;
        }
        return null;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
