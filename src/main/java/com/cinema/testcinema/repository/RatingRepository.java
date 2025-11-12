package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByUserIdAndMovieId(Long userId, Long movieId);
    Page<Rating> findByMovieId(Long movieId, Pageable pageable);
    boolean existsByUserIdAndMovieId(Long userId, Long movieId);
}
