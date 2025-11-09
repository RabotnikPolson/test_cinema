package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByUserIdAndMovieId(Long userId, Long movieId);
    List<Rating> findByMovieId(Long movieId);
    boolean existsByUserIdAndMovieId(Long userId, Long movieId);
}
