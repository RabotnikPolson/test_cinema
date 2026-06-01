package com.cinema.testcinema.service;

import com.cinema.testcinema.client.TmdbClient;
import com.cinema.testcinema.model.Genre;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.GenreRepository;
import com.cinema.testcinema.repository.MovieRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class KinopoiskSyncService {

    private static final Logger log = LoggerFactory.getLogger(KinopoiskSyncService.class);

    private final KinopoiskClient client;
    private final TmdbClient tmdbClient;
    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;

    public KinopoiskSyncService(KinopoiskClient client,
                                 TmdbClient tmdbClient,
                                 MovieRepository movieRepository,
                                 GenreRepository genreRepository) {
        this.client = client;
        this.tmdbClient = tmdbClient;
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Movie fetchAndSave(String kinopoiskId) {
        JsonNode data = client.fetchFilm(kinopoiskId);

        Movie movie = movieRepository.findByKinopoiskId(kinopoiskId)
                .orElse(new Movie());

        movie.setKinopoiskId(kinopoiskId);
        movie.setKinopoiskHdId(textOf(data, "kinopoiskHDId"));
        String imdbId = textOf(data, "imdbId");
        if (imdbId != null && movie.getImdbId() == null) movie.setImdbId(imdbId);

        if (movie.getImdbId() == null) {
            log.info("No IMDb ID from Kinopoisk for '{}'. Searching in TMDB...", movie.getTitle() != null ? movie.getTitle() : textOf(data, "nameRu"));
            String searchTitle = firstNonBlank(textOf(data, "nameEn"), textOf(data, "nameOriginal"), textOf(data, "nameRu"));
            Integer searchYear = data.hasNonNull("year") ? data.get("year").asInt() : null;
            Long tmdbId = tmdbClient.searchMovieId(searchTitle, searchYear);
            movie.setTmdbId(tmdbId);
        }

        movie.setTitle(firstNonBlank(
                textOf(data, "nameRu"),
                textOf(data, "nameEn"),
                textOf(data, "nameOriginal"),
                "Bez nazvaniya"
        ));
        movie.setNameEn(textOf(data, "nameEn"));
        movie.setNameOriginal(textOf(data, "nameOriginal"));

        movie.setPosterUrl(textOf(data, "posterUrl"));
        movie.setCoverUrl(textOf(data, "coverUrl"));
        movie.setLogoUrl(textOf(data, "logoUrl"));

        movie.setDescription(textOf(data, "description"));
        movie.setShortDescription(textOf(data, "shortDescription"));
        movie.setSlogan(textOf(data, "slogan"));
        movie.setEditorAnnotation(textOf(data, "editorAnnotation"));

        Long year = null;
        if (data.hasNonNull("year") && data.get("year").asLong() > 0) {
            year = data.get("year").asLong();
        } else if (data.hasNonNull("startYear") && data.get("startYear").asLong() > 0) {
            year = data.get("startYear").asLong();
        }
        
        if (year != null && (year < 1888 || year > 2100)) {
            year = null;
        }
        movie.setYear(year);

        if (data.hasNonNull("filmLength")) {
            JsonNode fl = data.get("filmLength");
            movie.setRuntime(fl.isNumber() ? fl.asInt() + " min" : fl.asText());
        }

        movie.setContentType(textOf(data, "type"));

        if (data.hasNonNull("ratingKinopoisk"))
            movie.setRatingKinopoisk(BigDecimal.valueOf(data.get("ratingKinopoisk").asDouble()));
        if (data.hasNonNull("ratingKinopoiskVoteCount"))
            movie.setRatingKinopoiskVoteCount(data.get("ratingKinopoiskVoteCount").asInt());
        if (data.hasNonNull("ratingImdb"))
            movie.setImdbRating(data.get("ratingImdb").asText());
        if (data.hasNonNull("ratingImdbVoteCount"))
            movie.setRatingImdbVoteCount(data.get("ratingImdbVoteCount").asInt());

        movie.setRatingMpaa(textOf(data, "ratingMpaa"));
        String ageLimit = textOf(data, "ratingAgeLimits");
        if (ageLimit != null) {
            String digits = ageLimit.replaceAll("[^0-9]", "");
            movie.setRatingAge(digits.isEmpty() ? ageLimit : digits + "+");
        }

        if (data.hasNonNull("countries") && data.get("countries").isArray()
                && !data.get("countries").isEmpty()) {
            StringBuilder countryBuilder = new StringBuilder();
            for (JsonNode c : data.get("countries")) {
                String name = c.path("country").asText("").trim();
                if (!name.isEmpty()) {
                    if (!countryBuilder.isEmpty()) countryBuilder.append(", ");
                    countryBuilder.append(name);
                }
            }
            if (!countryBuilder.isEmpty()) {
                movie.setCountry(countryBuilder.toString());
            }
        }

        movie.setProductionStatus(textOf(data, "productionStatus"));
        movie.setSerial(boolOf(data, "serial"));
        movie.setShortFilm(boolOf(data, "shortFilm"));
        movie.setHasImax(boolOf(data, "hasImax"));
        movie.setHas3d(boolOf(data, "has3D"));

        String country = movie.getCountry();
        if (country != null) {
            String countryLower = country.toLowerCase();
            boolean domestic = countryLower.contains("казахстан")
                    || countryLower.contains("kazakhstan")
                    || countryLower.contains("казсср")
                    || countryLower.contains("қазсср")
                    || countryLower.contains("қазақстан")
                    || countryLower.contains("kazssr");

            movie.setDomestic(domestic);
            movie.setKzCulturalWeight(domestic ? 5 : 1);
        }

        movie.getGenres().clear();
        if (data.hasNonNull("genres") && data.get("genres").isArray()) {
            StringBuilder genreText = new StringBuilder();
            for (JsonNode g : data.get("genres")) {
                String genreName = capitalize(g.path("genre").asText("").trim());
                if (genreName.isBlank()) continue;
                if (!genreText.isEmpty()) genreText.append(", ");
                genreText.append(genreName);
                Genre genre = genreRepository.findByName(genreName);
                if (genre == null) genre = genreRepository.save(new Genre(genreName));
                movie.getGenres().add(genre);
            }
            movie.setGenreText(genreText.toString());
        }

        try {
            JsonNode staffArray = client.fetchStaff(kinopoiskId);
            if (staffArray != null && staffArray.isArray()) {
                List<String> directors = new ArrayList<>();
                List<String> actors = new ArrayList<>();
                for (JsonNode person : staffArray) {
                    String profKey = person.path("professionKey").asText("");
                    String name = firstNonBlank(
                            person.path("nameRu").asText("").trim(),
                            person.path("nameEn").asText("").trim()
                    );
                    if (name == null || name.isBlank()) continue;
                    if ("DIRECTOR".equalsIgnoreCase(profKey)) directors.add(name);
                    else if ("ACTOR".equalsIgnoreCase(profKey) && actors.size() < 7) actors.add(name);
                }
                if (!directors.isEmpty()) movie.setDirector(String.join(", ", directors));
                if (!actors.isEmpty()) movie.setActors(String.join(", ", actors));
            }
        } catch (Exception e) {
            log.warn("Could not load staff for kinopoiskId={}: {}", kinopoiskId, e.getMessage());
        }

        Movie saved = movieRepository.save(movie);
        log.info("Synced: {} | kpRating={} | isDomestic={} | dir={}",
                saved.getTitle(), saved.getRatingKinopoisk(), saved.isDomestic(), saved.getDirector());
        return saved;
    }

    private String textOf(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) return null;
        String val = node.get(field).asText("").trim();
        return val.isEmpty() ? null : val;
    }

    private boolean boolOf(JsonNode node, String field) {
        return node != null && node.has(field) && node.get(field).asBoolean(false);
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
