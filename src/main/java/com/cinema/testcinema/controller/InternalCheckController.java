package com.cinema.testcinema.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class InternalCheckController {
    private final JdbcTemplate jdbc;

    public InternalCheckController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/internal/count-movies")
    public Map<String,Object> countMovies() {
        try {
            Integer c = jdbc.queryForObject("select count(*) from movies", Integer.class);
            return Map.of("ok", true, "count", c);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    @GetMapping("/internal/count-favs")
    public Map<String,Object> countFavs() {
        try {
            Integer c = jdbc.queryForObject("select count(*) from user_favorites", Integer.class);
            return Map.of("ok", true, "count", c);
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }
}
