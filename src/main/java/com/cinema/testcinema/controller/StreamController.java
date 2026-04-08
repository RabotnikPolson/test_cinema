package com.cinema.testcinema.controller;

import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.repository.MovieRepository;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/stream")
public class StreamController {

    private static final Pattern DS_LANG_PATTERN = Pattern.compile("^[a-z]{2}(-[A-Z]{2})?$");

    private final MovieRepository movieRepository;
    private final String vidsrcBaseUrl;

    public StreamController(MovieRepository movieRepository,
            @Value("${vidsrc.base-url}") String vidsrcBaseUrl) {
        this.movieRepository = movieRepository;
        this.vidsrcBaseUrl = vidsrcBaseUrl;
    }

    @GetMapping("/{id}")
    @PermitAll
    public Map<String, String> stream(@PathVariable Long id,
            @RequestParam(value = "ds_lang", required = false) String dsLang,
            @RequestParam(value = "autoplay", required = false) String autoplay,
            @RequestParam(value = "sub_url", required = false) String subUrl) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм с ID " + id + " не найден"));

        String kinopoiskId = movie.getKinopoiskId();
        if (kinopoiskId != null && !kinopoiskId.isBlank()) {
            return Map.of(
                    "type", "embed",
                    "url", "https://vbdkv.com/api/short/" + kinopoiskId);
        }

        String imdbId = movie.getImdbId();
        if (imdbId == null || imdbId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "У фильма отсутствует imdbId или kinopoiskId для построения embed URL");
        }

        validateDsLang(dsLang);
        validateAutoplay(autoplay);
        validateSubUrl(subUrl);

        String embedUrl = buildEmbedUrl(imdbId, dsLang, autoplay, subUrl);
        return Map.of(
                "type", "embed",
                "url", embedUrl);
    }

    private String buildEmbedUrl(String imdbId, String dsLang, String autoplay, String subUrl) {
        String normalizedBase = vidsrcBaseUrl.endsWith("/")
                ? vidsrcBaseUrl.substring(0, vidsrcBaseUrl.length() - 1)
                : vidsrcBaseUrl;

        List<String> query = new ArrayList<>();
        query.add("imdb=" + encode(imdbId));
        if (dsLang != null && !dsLang.isBlank()) {
            query.add("ds_lang=" + encode(dsLang));
        }
        if (autoplay != null && !autoplay.isBlank()) {
            query.add("autoplay=" + autoplay);
        }
        if (subUrl != null && !subUrl.isBlank()) {
            query.add("sub_url=" + encode(subUrl));
        }

        return normalizedBase + "/embed/movie?" + String.join("&", query);
    }

    private void validateDsLang(String dsLang) {
        if (dsLang == null || dsLang.isBlank()) {
            return;
        }
        if (!DS_LANG_PATTERN.matcher(dsLang).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Параметр ds_lang должен быть в формате ll или ll-RR (например, en или pt-BR)");
        }
    }

    private void validateAutoplay(String autoplay) {
        if (autoplay == null || autoplay.isBlank()) {
            return;
        }
        if (!"0".equals(autoplay) && !"1".equals(autoplay)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Параметр autoplay должен быть только 0 или 1");
        }
    }

    private void validateSubUrl(String subUrl) {
        if (subUrl == null || subUrl.isBlank()) {
            return;
        }
        try {
            URI uri = new URI(subUrl);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Параметр sub_url должен быть http/https URL");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Параметр sub_url должен быть валидным URL");
            }
        } catch (URISyntaxException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Параметр sub_url должен быть валидным URL", ex);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
