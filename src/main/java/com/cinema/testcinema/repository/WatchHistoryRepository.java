package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {
    Optional<WatchHistory> findTopByUserIdAndMovieIdAndSessionIdOrderByIdDesc(Long userId, Long movieId,
            UUID sessionId);

    @Query(value = "SELECT COALESCE(SUM(seconds_watched), 0) FROM watch_history WHERE user_id = :userId", nativeQuery = true)
    Long getTotalSecondsWatched(@Param("userId") Long userId);

    @Query(value = "SELECT g.name AS genre, CAST(SUM(wh.seconds_watched) AS BIGINT) AS totalSeconds " +
            "FROM watch_history wh " +
            "JOIN movies m ON wh.movie_id = m.id " +
            "JOIN movie_genres mg ON m.id = mg.movie_id " +
            "JOIN genres g ON mg.genre_id = g.id " +
            "WHERE wh.user_id = :userId " +
            "GROUP BY g.name " +
            "ORDER BY totalSeconds DESC " +
            "LIMIT 8", nativeQuery = true)
    List<Object[]> getTopGenresWatched(@Param("userId") Long userId);

    @Query(value = "SELECT DATE(started_at) AS watchDate, CAST(SUM(seconds_watched) AS BIGINT) AS totalSeconds " +
            "FROM watch_history " +
            "WHERE user_id = :userId " +
            "GROUP BY DATE(started_at) " +
            "ORDER BY watchDate ASC", nativeQuery = true)
    List<Object[]> getActivityByDay(@Param("userId") Long userId);

    @Query(value = "SELECT COALESCE(SUM(seconds_watched), 0) FROM watch_history", nativeQuery = true)
    long getTotalWatchSecondsAllUsers();

    @Query(value = """
            SELECT m.id, m.title, m.poster_url, m.is_domestic,
                   SUM(wh.seconds_watched) as score
            FROM watch_history wh
            JOIN movies m ON wh.movie_id = m.id
            WHERE wh.started_at > :since
            GROUP BY m.id, m.title, m.poster_url, m.is_domestic
            ORDER BY score DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopByWatchTime(@Param("since") Instant since, @Param("limit") int limit);

    @Query(value = """
            SELECT COALESCE(SUM(wh.seconds_watched), 0)
            FROM watch_history wh
            JOIN movies m ON wh.movie_id = m.id
            WHERE m.is_domestic = true
            """, nativeQuery = true)
    long getTotalDomesticWatchSeconds();

    @Query(value = """
            SELECT COALESCE(SUM(wh.seconds_watched), 0)
            FROM watch_history wh
            JOIN movies m ON wh.movie_id = m.id
            WHERE m.is_domestic = false
            """, nativeQuery = true)
    long getTotalForeignWatchSeconds();
}
