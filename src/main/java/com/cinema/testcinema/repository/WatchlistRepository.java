package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.Watchlist;
import com.cinema.testcinema.model.Watchlist.WatchlistId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WatchlistRepository extends JpaRepository<Watchlist, WatchlistId> {
    boolean existsByUserIdAndMovieId(Long userId, Long movieId);
    void deleteByUserIdAndMovieId(Long userId, Long movieId);
    List<Watchlist> findByUserId(Long userId);
}
