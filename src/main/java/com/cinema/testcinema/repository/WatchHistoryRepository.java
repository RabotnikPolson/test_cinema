package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {
    Optional<WatchHistory> findTopByUserIdAndMovieIdAndSessionIdOrderByIdDesc(Long userId, Long movieId, UUID sessionId);
}
