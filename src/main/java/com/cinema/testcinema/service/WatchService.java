package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.AnalyticsSummaryDto;
import com.cinema.testcinema.dto.WatchBeatDto;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.model.WatchHistory;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.WatchHistoryRepository;
import jakarta.transaction.Transactional;
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

    @Transactional
    public AnalyticsSummaryDto mySummary(Long userId) {
        // total
        long total = whRepo.findAll().stream()
                .filter(w -> w.getUser()!=null && Objects.equals(w.getUser().getId(), userId))
                .mapToLong(WatchHistory::getSecondsWatched)
                .sum();

        // genres pie (через Movie.genreText)
        Map<String, Long> byGenre = new HashMap<>();
        whRepo.findAll().stream()
                .filter(w -> w.getUser()!=null && Objects.equals(w.getUser().getId(), userId))
                .forEach(w -> {
                    for (String g : extractGenres(w.getMovie())) {
                        byGenre.merge(g, (long) w.getSecondsWatched(), Long::sum);
                    }
                });
        List<AnalyticsSummaryDto.Item> genresPie = byGenre.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(e -> new AnalyticsSummaryDto.Item(e.getKey(), e.getValue()))
                .toList();

        // activity by day
        Map<LocalDate, Long> byDay = new HashMap<>();
        whRepo.findAll().stream()
                .filter(w -> w.getUser()!=null && Objects.equals(w.getUser().getId(), userId))
                .forEach(w -> {
                    LocalDate d = w.getStartedAt().atZone(ZoneOffset.UTC).toLocalDate();
                    byDay.merge(d, (long) w.getSecondsWatched(), Long::sum);
                });
        List<AnalyticsSummaryDto.Point> points = byDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new AnalyticsSummaryDto.Point(e.getKey().toString(), e.getValue()))
                .toList();

        return new AnalyticsSummaryDto(total, genresPie, points);
    }

    private static List<String> extractGenres(Movie m) {
        String raw = m.getGenreText();
        if (raw == null || raw.isBlank()) return List.of();
        String[] parts = raw.split("[,/|;]");
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            String s = p.trim();
            if (!s.isBlank()) out.add(s);
        }
        return out;
    }
}
