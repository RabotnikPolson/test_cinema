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

    @Query(value = """
            SELECT m.id, m.title, m.poster_url, m.is_domestic,
                   COUNT(mc.id) as score
            FROM movie_clicks mc
            JOIN movies m ON mc.movie_id = m.id
            WHERE mc.clicked_at > :since
            GROUP BY m.id, m.title, m.poster_url, m.is_domestic
            ORDER BY score DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopByClicks(@Param("since") Instant since, @Param("limit") int limit);

    @Query(value = """
            SELECT m.id, m.title, m.poster_url, m.is_domestic,
                   COUNT(mc.id) as score
            FROM movie_clicks mc
            JOIN movies m ON mc.movie_id = m.id
            WHERE mc.clicked_at > :since
              AND m.is_domestic = true
            GROUP BY m.id, m.title, m.poster_url, m.is_domestic
            ORDER BY score DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopDomesticByClicks(@Param("since") Instant since, @Param("limit") int limit);
}
