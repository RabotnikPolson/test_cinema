package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.AnalyticsSummaryDto;
import com.cinema.testcinema.dto.UserProfileStatsDto;
import com.cinema.testcinema.dto.WatchBeatDto;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.model.WatchHistory;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.WatchHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class WatchService {
    private static final Logger log = LoggerFactory.getLogger(WatchService.class);
    private final WatchHistoryRepository whRepo;
    private final MovieRepository movieRepo;
    private final JdbcTemplate jdbc;

    public WatchService(WatchHistoryRepository whRepo, MovieRepository movieRepo, JdbcTemplate jdbc) {
        this.whRepo = whRepo;
        this.movieRepo = movieRepo;
        this.jdbc = jdbc;
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

        // Автоматически помечаем просмотр завершённым при достижении 90% хронометража
        if (!wh.isCompleted()) {
            Integer runtimeMinutes = parseRuntimeMinutes(movie.getRuntime());
            if (runtimeMinutes != null && runtimeMinutes > 0) {
                int runtimeSeconds = runtimeMinutes * 60;
                if (wh.getSecondsWatched() >= runtimeSeconds * 0.85) {
                    wh.setCompleted(true);
                    log.debug("[WATCH] session {} marked as completed ({} sec / {} sec)",
                            dto.sessionId(), wh.getSecondsWatched(), runtimeSeconds);
                }
            }
        }

        whRepo.save(wh);
    }

    /**
     * Парсит хронометраж фильма из строки.
     * Поддерживаемые форматы: "120 min", "120"
     *
     * @param runtime строка из Movie.runtime
     * @return длительность в минутах, или null если строка null / непарсируемая
     */
    private Integer parseRuntimeMinutes(String runtime) {
        if (runtime == null || runtime.isBlank()) {
            return null;
        }
        try {
            // Формат "120 min" — берём первый токен до пробела
            String trimmed = runtime.trim().split("\\s+")[0];
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            log.debug("[WATCH] Не удалось распарсить runtime '{}', пропускаем расчёт completion", runtime);
            return null;
        }
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

    @Transactional(readOnly = true)
    public UserProfileStatsDto myProfile(Long userId) {
        long totalSeconds = Optional.ofNullable(whRepo.getTotalSecondsWatched(userId)).orElse(0L);

        Integer completedMovies = jdbc.queryForObject(
                "SELECT COUNT(*) FROM watch_history WHERE user_id = ? AND completed = true", Integer.class, userId);

        Integer kazakhstanMovies = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT wh.movie_id)
                FROM watch_history wh
                JOIN movies m ON m.id = wh.movie_id
                WHERE wh.user_id = ? AND m.is_domestic = true
                """, Integer.class, userId);

        Integer streak = jdbc.queryForObject("""
                WITH consecutive AS (
                    SELECT DISTINCT CAST(started_at AS DATE) AS d
                    FROM watch_history WHERE user_id = ?
                ),
                ordered AS (
                    SELECT d, ROW_NUMBER() OVER (ORDER BY d DESC) AS rn FROM consecutive
                )
                SELECT COUNT(*) FROM ordered
                WHERE d = CURRENT_DATE - CAST(rn - 1 AS INT)
                """, Integer.class, userId);

        String favoriteGenre = null;
        List<Object[]> topGenres = whRepo.getTopGenresWatched(userId);
        if (topGenres != null && !topGenres.isEmpty()) {
            favoriteGenre = (String) topGenres.get(0)[0];
        }

        String favoriteDirector = null;
        List<Map<String, Object>> directorRows = jdbc.queryForList("""
                SELECT m.director, SUM(wh.seconds_watched) AS total
                FROM watch_history wh JOIN movies m ON m.id = wh.movie_id
                WHERE wh.user_id = ? AND m.director IS NOT NULL AND m.director != ''
                GROUP BY m.director ORDER BY total DESC LIMIT 1
                """, userId);
        if (!directorRows.isEmpty()) {
            favoriteDirector = (String) directorRows.get(0).get("director");
        }

        Integer userRatings = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ratings WHERE user_id = ?", Integer.class, userId);

        Integer nightOwlCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM watch_history
                WHERE user_id = ? AND EXTRACT(HOUR FROM started_at) >= 23
                """, Integer.class, userId);

        List<String> achievements = new ArrayList<>();
        if (Optional.ofNullable(kazakhstanMovies).orElse(0) >= 5)  achievements.add("Казахстанец");
        if (Optional.ofNullable(completedMovies).orElse(0) >= 20)  achievements.add("Киноман");
        if (Optional.ofNullable(userRatings).orElse(0) >= 15)      achievements.add("Критик");
        if (Optional.ofNullable(streak).orElse(0) >= 3)            achievements.add("Марафонщик");
        if (Optional.ofNullable(nightOwlCount).orElse(0) >= 5)     achievements.add("Ночная сова");

        return new UserProfileStatsDto(
                totalSeconds,
                Optional.ofNullable(completedMovies).orElse(0),
                Optional.ofNullable(kazakhstanMovies).orElse(0),
                Optional.ofNullable(streak).orElse(0),
                favoriteGenre,
                favoriteDirector,
                achievements
        );
    }
}
