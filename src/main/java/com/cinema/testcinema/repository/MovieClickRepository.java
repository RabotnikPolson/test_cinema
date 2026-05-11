package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.MovieClick;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MovieClickRepository extends JpaRepository<MovieClick, Long> {

    @Query("SELECT c.movie as movie, COUNT(c) as clicks FROM MovieClick c " +
           "WHERE c.clickedAt > :since GROUP BY c.movie ORDER BY clicks DESC")
    List<TrendingClickProjection> findTopTrendingMovies(@Param("since") Instant since, Pageable pageable);
}
