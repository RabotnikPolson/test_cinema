package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.metrics.MovieClickRequest;
import com.cinema.testcinema.dto.metrics.SearchLogRequest;
import com.cinema.testcinema.dto.metrics.SubtitleEventRequest;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.MovieClick;
import com.cinema.testcinema.model.SearchLog;
import com.cinema.testcinema.model.SubtitleEvent;
import com.cinema.testcinema.repository.MovieClickRepository;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.SearchLogRepository;
import com.cinema.testcinema.repository.SubtitleEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MetricsService {
    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

    private final MovieClickRepository clickRepo;
    private final SearchLogRepository searchRepo;
    private final SubtitleEventRepository subtitleRepo;
    private final MovieRepository movieRepo;
    private final TrendingRedisService trendingRedisService;

    public MetricsService(MovieClickRepository clickRepo,
                          SearchLogRepository searchRepo,
                          SubtitleEventRepository subtitleRepo,
                          MovieRepository movieRepo,
                          TrendingRedisService trendingRedisService) {
        this.clickRepo = clickRepo;
        this.searchRepo = searchRepo;
        this.subtitleRepo = subtitleRepo;
        this.movieRepo = movieRepo;
        this.trendingRedisService = trendingRedisService;
    }

    @Transactional
    public void logClick(MovieClickRequest request, Long userId) {
        Movie movieRef = movieRepo.findById(request.movieId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден"));
        MovieClick click = new MovieClick(userId, request.guestSessionId(), movieRef);
        clickRepo.save(click);
        
        // Увеличиваем счетчик в Redis для ленты трендов
        trendingRedisService.incrementMovieClick(request.movieId());
        
        log.debug("[METRICS] click: userId={}, guestSession={}, movieId={}", 
                userId, request.guestSessionId(), request.movieId());
    }

    @Transactional
    public void logSearch(SearchLogRequest request, Long userId) {
        SearchLog entry = new SearchLog(userId, request.guestSessionId(), request.query(), request.resultCount());
        searchRepo.save(entry);
        log.debug("[METRICS] search: userId={}, guestSession={}, query='{}', results={}", 
                userId, request.guestSessionId(), request.query(), request.resultCount());
    }

    @Transactional
    public void logSubtitleEvent(SubtitleEventRequest request, Long userId) {
        Movie movieRef = movieRepo.findById(request.movieId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден"));
        SubtitleEvent event = new SubtitleEvent(userId, request.guestSessionId(), movieRef, request.action(), request.lang());
        subtitleRepo.save(event);
        log.debug("[METRICS] subtitle_event: userId={}, guestSession={}, movieId={}, action={}, lang={}",
                userId, request.guestSessionId(), request.movieId(), request.action(), request.lang());
    }
}
