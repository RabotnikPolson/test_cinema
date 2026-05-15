package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    Movie findByImdbId(String imdbId);

    Optional<Movie> findByKinopoiskId(String kinopoiskId);
}



