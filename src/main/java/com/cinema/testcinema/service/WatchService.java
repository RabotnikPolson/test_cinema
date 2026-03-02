package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.AnalyticsSummaryDto;
import com.cinema.testcinema.dto.WatchBeatDto;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.model.WatchHistory;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.UserRepository;
import com.cinema.testcinema.repository.WatchHistoryRepository;
import com.cinema.testcinema.security.AuthenticatedUserService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WatchService {

    private final WatchHistoryRepository whRepo;
    private final MovieRepository movieRepo;
    private final UserRepository userRepo;
    private final AuthenticatedUserService authenticatedUserService;

    public WatchService(
            WatchHistoryRepository whRepo,
            MovieRepository movieRepo,
            UserRepository userRepo,
            AuthenticatedUserService authenticatedUserService) {
        this.whRepo = whRepo;
        this.movieRepo = movieRepo;
        this.userRepo = userRepo;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Transactional
    public void beat(WatchBeatDto dto) {
        Long userId = authenticatedUserService.requireCurrentUserId();
        User user = userRepo.findById(userId).orElseThrow();
        Movie movie = movieRepo.findById(dto.movieId()).orElseThrow();
        UUID sessionId = UUID.fromString(dto.sessionId());
        WatchHistory wh = whRepo
                .findTopByUserIdAndMovieIdAndSessionIdOrderByIdDesc(
                        user.getId(),
                        movie.getId(),
                        sessionId
                )
                .orElseGet(() -> new WatchHistory(user, movie, sessionId));


        int delta = Math.min(Math.max(dto.deltaSec(), 0), 30);
        wh.setSecondsWatched(wh.getSecondsWatched() + delta);
        wh.setLastPositionSec(dto.currentPositionSec());

        if (movie.getDurationSeconds() != null && movie.getDurationSeconds() > 0) {
            if ((double) wh.getSecondsWatched() / movie.getDurationSeconds() >= 0.9) {
                wh.setCompleted(true);
            }
        }

        whRepo.save(wh);
    }

    @Transactional
    public AnalyticsSummaryDto mySummary(Long userId) {
        List<WatchHistory> userHistories = whRepo.findAll().stream()
                .filter(w -> w.getUser() != null && Objects.equals(w.getUser().getId(), userId))
                .collect(Collectors.toList());

        long total = userHistories.stream()
                .mapToLong(WatchHistory::getSecondsWatched)
                .sum();

        Map<String, Long> byGenre = new HashMap<>();
        for (WatchHistory w : userHistories) {
            for (String g : extractGenres(w.getMovie())) {
                byGenre.merge(g, (long) w.getSecondsWatched(), Long::sum);
            }
        }

        List<AnalyticsSummaryDto.Item> genresPie = byGenre.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(e -> new AnalyticsSummaryDto.Item(e.getKey(), e.getValue()))
                .toList();

        return new AnalyticsSummaryDto(total, genresPie, List.of());
    }

    private static List<String> extractGenres(Movie m) {
        if (m.getGenres() != null && !m.getGenres().isEmpty()) {
            return m.getGenres().stream()
                    .map(g -> g.getName() == null ? "" : g.getName().trim())
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .toList();
        }
        String raw = m.getGenreText();
        if (raw == null || raw.isBlank()) return List.of();
        List<String> out = new ArrayList<>();
        for (String p : raw.split("[,/|;]")) {
            String s = p.trim();
            if (!s.isBlank()) out.add(s);
        }
        return out;
    }
}