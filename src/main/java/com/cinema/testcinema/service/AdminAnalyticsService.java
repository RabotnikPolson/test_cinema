package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.analytics.*;
import com.cinema.testcinema.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AdminAnalyticsService {

    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final MovieClickRepository movieClickRepository;
    private final RatingRepository ratingRepository;
    private final MovieSubtitleRepository movieSubtitleRepository;

    public AdminAnalyticsService(MovieRepository movieRepository,
                                 UserRepository userRepository,
                                 WatchHistoryRepository watchHistoryRepository,
                                 MovieClickRepository movieClickRepository,
                                 RatingRepository ratingRepository,
                                 MovieSubtitleRepository movieSubtitleRepository) {
        this.movieRepository = movieRepository;
        this.userRepository = userRepository;
        this.watchHistoryRepository = watchHistoryRepository;
        this.movieClickRepository = movieClickRepository;
        this.ratingRepository = ratingRepository;
        this.movieSubtitleRepository = movieSubtitleRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardDto getDashboard(int topLimit, String period) {
        Instant since = "month".equals(period)
                ? Instant.now().minus(30, ChronoUnit.DAYS)
                : Instant.now().minus(7, ChronoUnit.DAYS);

        // Блок 1 — Overview
        long totalMovies = movieRepository.count();
        long domesticMovies = movieRepository.countDomestic();
        long foreignMovies = movieRepository.countForeign();
        long totalUsers = userRepository.count();
        long totalWatchSeconds = watchHistoryRepository.getTotalWatchSecondsAllUsers();
        long translatedSubtitles = movieSubtitleRepository.countByTranslationStatus("success");

        AdminOverviewDto overview = new AdminOverviewDto(
                totalMovies, domesticMovies, foreignMovies,
                totalUsers, totalWatchSeconds / 3600,
                translatedSubtitles
        );

        // Блок 2 — Топы
        List<TopMovieDto> topByClicks = movieClickRepository
                .findTopByClicks(since, topLimit)
                .stream().map(this::mapToTopMovie).toList();

        List<TopMovieDto> topByWatchTime = watchHistoryRepository
                .findTopByWatchTime(since, topLimit)
                .stream().map(this::mapToTopMovie).toList();

        List<TopMovieDto> topRated = ratingRepository
                .findTopRated(topLimit)
                .stream().map(this::mapToTopMovie).toList();

        List<TopMovieDto> topDomesticByClicks = movieClickRepository
                .findTopDomesticByClicks(since, topLimit)
                .stream().map(this::mapToTopMovie).toList();

        // Блок 3 — Соотношение казахский/зарубежный
        long domesticSeconds = watchHistoryRepository.getTotalDomesticWatchSeconds();
        long foreignSeconds = watchHistoryRepository.getTotalForeignWatchSeconds();
        long totalSeconds = domesticSeconds + foreignSeconds;
        double domesticPercent = totalSeconds > 0
                ? Math.round((double) domesticSeconds / totalSeconds * 1000.0) / 10.0
                : 0.0;
        double foreignPercent = totalSeconds > 0
                ? 100.0 - domesticPercent
                : 0.0;

        ContentRatioDto contentRatio = new ContentRatioDto(
                domesticSeconds, foreignSeconds, domesticPercent, foreignPercent
        );

        // Блок 3 — Очередь субтитров
        List<Object[]> queueStats = movieSubtitleRepository.getSubtitleQueueStats();
        Object[] q = queueStats.isEmpty() ? new Object[5] : queueStats.get(0);
        SubtitleQueueDto subtitleQueue = new SubtitleQueueDto(
                q[0] != null ? ((Number) q[0]).longValue() : 0,
                q[1] != null ? ((Number) q[1]).longValue() : 0,
                q[2] != null ? ((Number) q[2]).longValue() : 0,
                q[3] != null ? ((Number) q[3]).longValue() : 0,
                q[4] != null ? ((Number) q[4]).longValue() : 0
        );

        return new AdminDashboardDto(
                overview, topByClicks, topByWatchTime,
                topRated, topDomesticByClicks,
                contentRatio, subtitleQueue
        );
    }

    private TopMovieDto mapToTopMovie(Object[] row) {
        return new TopMovieDto(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (Boolean) row[3],
                ((Number) row[4]).longValue()
        );
    }

    @Transactional(readOnly = true)
    public List<SubtitleQueueRowDto> getSubtitleQueueRows() {
        return mapQueueRows(movieSubtitleRepository.getSubtitleQueueRows());
    }

    @Transactional(readOnly = true)
    public List<SubtitleQueueRowDto> getDownloadQueueRows() {
        return mapQueueRows(movieSubtitleRepository.getDownloadQueueRows());
    }

    private List<SubtitleQueueRowDto> mapQueueRows(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new SubtitleQueueRowDto(
                        ((Number) row[0]).longValue(),
                        ((Number) row[1]).longValue(),
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        row[5] != null ? row[5].toString() : null,
                        row[6] != null ? ((Number) row[6]).intValue() : 0
                ))
                .toList();
    }
}
