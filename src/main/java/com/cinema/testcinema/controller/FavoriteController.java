package com.cinema.testcinema.controller;

import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.service.FavoriteService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/favorites")
public class FavoriteController {

    private final FavoriteService service;

    public FavoriteController(FavoriteService service) {
        this.service = service;
    }

    @GetMapping
    public List<Movie> list(Authentication auth) {
        return service.listForUsername(auth.getName());
    }

    @PutMapping("/{imdbId}")
    public void add(@PathVariable String imdbId, Authentication auth) {
        service.addByImdb(auth.getName(), imdbId);
    }

    @DeleteMapping("/{imdbId}")
    public void remove(@PathVariable String imdbId, Authentication auth) {
        service.removeByImdb(auth.getName(), imdbId);
    }
}
