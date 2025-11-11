package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @EntityGraph(attributePaths = {"user"})
    Page<Review> findByMovieImdbIdOrderByCreatedAtDesc(String imdbId, Pageable p);

    Optional<Review> findByMovieIdAndUserId(Long movieId, Long userId);
}
