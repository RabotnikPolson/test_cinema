package com.cinema.testcinema.service;

import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.repository.FavoriteRepository;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteService {
    private final UserRepository users;
    private final MovieRepository movies;
    private final FavoriteRepository favRepo;

    public FavoriteService(UserRepository users, MovieRepository movies, FavoriteRepository favRepo) {
        this.users = users;
        this.movies = movies;
        this.favRepo = favRepo;
    }

    public List<Movie> listForUsername(String username) {
        User u = users.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return favRepo.findFavoritesByUserId(u.getId());
    }

    @Transactional
    public void addByImdb(String username, String imdbId) {
        User u = users.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Movie m = movies.findByImdbId(imdbId).orElseThrow(() -> new IllegalArgumentException("Movie not found"));
        favRepo.insertIfNotExists(u.getId(), m.getId());
    }

    @Transactional
    public void removeByImdb(String username, String imdbId) {
        User u = users.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Movie m = movies.findByImdbId(imdbId).orElseThrow(() -> new IllegalArgumentException("Movie not found"));
        favRepo.deleteLink(u.getId(), m.getId());
    }
}
