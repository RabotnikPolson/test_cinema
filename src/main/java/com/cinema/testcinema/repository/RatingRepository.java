package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByUserIdAndMovieId(Long userId, Long movieId);
    Page<Rating> findByMovieId(Long movieId, Pageable pageable);
    boolean existsByUserIdAndMovieId(Long userId, Long movieId);
    Page<Rating> findByUserId(Long userId, Pageable pageable);

    @Query(value = """
            SELECT m.id, m.title, m.poster_url, m.is_domestic,
                   CAST(AVG(r.score) * 100 AS BIGINT) as score
            FROM ratings r
            JOIN movies m ON r.movie_id = m.id
            GROUP BY m.id, m.title, m.poster_url, m.is_domestic
            HAVING COUNT(r.id) >= 3
            ORDER BY score DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopRated(@Param("limit") int limit);
}
