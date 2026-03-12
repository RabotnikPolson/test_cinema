package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.AnalyticsSummaryDto;
import com.cinema.testcinema.dto.WatchBeatDto;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.model.WatchHistory;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.WatchHistoryRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class WatchService {
    private final WatchHistoryRepository whRepo;
    private final MovieRepository movieRepo;

    public WatchService(WatchHistoryRepository whRepo, MovieRepository movieRepo) {
        this.whRepo = whRepo;
        this.movieRepo = movieRepo;
    }

    @Transactional
    public void beat(User user, WatchBeatDto dto) {
        int delta = Math.min(Math.max(dto.deltaSec(), 0), 30);

        Movie movie = movieRepo.findById(dto.movieId())
                .orElseThrow(() -> new NoSuchElementException("movie not found"));

        WatchHistory wh = whRepo
                .findTopByUserIdAndMovieIdAndSessionIdOrderByIdDesc(user.getId(), movie.getId(), dto.sessionId())
                .orElseGet(() -> {
                    WatchHistory w = new WatchHistory();
                    w.setUser(user);
                    w.setMovie(movie);
                    w.setSessionId(dto.sessionId());
                    w.setStartedAt(Objects.requireNonNullElse(dto.clientTs(), Instant.now()));
                    w.setSecondsWatched(0);
                    return w;
                });

        if (!dto.paused()) {
            wh.setSecondsWatched(wh.getSecondsWatched() + delta);
        }
        wh.setLastBeatAt(Instant.now());
        whRepo.save(wh);
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryDto mySummary(Long userId) {
        // total
        Long totalWatches = whRepo.getTotalSecondsWatched(userId);
        long total = totalWatches != null ? totalWatches : 0L;

        // genres pie (using Native SQL for top 8 genres)
        List<Object[]> genresRaw = whRepo.getTopGenresWatched(userId);
        List<AnalyticsSummaryDto.Item> genresPie = new ArrayList<>();
        if (genresRaw != null) {
            for (Object[] row : genresRaw) {
                String genre = (String) row[0];
                Number seconds = (Number) row[1];
                genresPie.add(new AnalyticsSummaryDto.Item(genre, seconds != null ? seconds.longValue() : 0L));
            }
        }

        // activity by day (using Native SQL grouping by date)
        List<Object[]> daysRaw = whRepo.getActivityByDay(userId);
        List<AnalyticsSummaryDto.Point> points = new ArrayList<>();
        if (daysRaw != null) {
            for (Object[] row : daysRaw) {
                java.sql.Date sqlDate = (java.sql.Date) row[0];
                Number seconds = (Number) row[1];
                points.add(new AnalyticsSummaryDto.Point(
                        sqlDate.toString(),
                        seconds != null ? seconds.longValue() : 0L));
            }
        }

        return new AnalyticsSummaryDto(total, genresPie, points);
    }
}
