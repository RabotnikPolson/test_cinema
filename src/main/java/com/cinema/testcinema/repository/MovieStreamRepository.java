package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.MovieStream;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieStreamRepository extends JpaRepository<MovieStream, Long> {

    Optional<MovieStream> findByImdbId(String imdbId);

    void deleteByImdbId(String imdbId);
}
