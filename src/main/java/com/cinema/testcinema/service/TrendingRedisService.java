package com.cinema.testcinema.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TrendingRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ZSetOperations<String, Object> zSetOperations;

    public TrendingRedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.zSetOperations = redisTemplate.opsForZSet();
    }

    /**
     * Формирует ключ для текущей недели, например "trending:movies:2026-W20"
     */
    private String getWeeklyTrendingKey() {
        LocalDate now = LocalDate.now();
        int weekNumber = now.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
        return "trending:movies:" + now.getYear() + "-W" + weekNumber;
    }

    /**
     * Инкрементирует счетчик просмотров фильма в трендах.
     * Эту функцию нужно вызывать из контроллера при открытии карточки фильма.
     */
    public void incrementMovieClick(Long movieId) {
        String key = getWeeklyTrendingKey();
        zSetOperations.incrementScore(key, movieId.toString(), 1.0);
    }

    /**
     * Возвращает топ-N ID фильмов за текущую неделю.
     */
    public Set<Long> getTopTrendingIds(int limit) {
        String key = getWeeklyTrendingKey();
        // Возвращаем в обратном порядке (от большего score к меньшему)
        Set<Object> ids = zSetOperations.reverseRange(key, 0, limit - 1);
        if (ids == null) return Set.of();
        
        return ids.stream()
                .map(id -> Long.valueOf(id.toString()))
                .collect(Collectors.toSet());
    }
}
