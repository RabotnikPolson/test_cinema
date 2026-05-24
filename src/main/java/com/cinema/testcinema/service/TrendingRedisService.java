package com.cinema.testcinema.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
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

    private String getWeeklyTrendingKey() {
        LocalDate now = LocalDate.now();
        int weekNumber = now.get(WeekFields.ISO.weekOfWeekBasedYear());
        return "trending:movies:" + now.getYear() + "-W" + weekNumber;
    }

    public void incrementMovieClick(Long movieId) {
        String key = getWeeklyTrendingKey();
        zSetOperations.incrementScore(key, movieId.toString(), 1.0);
    }

    /**
     * Возвращает топ-N ID фильмов за текущую неделю в порядке убывания score (Redis ZSET order).
     */
    public List<Long> getTopTrendingIds(int limit) {
        String key = getWeeklyTrendingKey();
        Set<Object> ids = zSetOperations.reverseRange(key, 0, limit - 1);
        if (ids == null) return List.of();

        return ids.stream()
                .map(id -> Long.valueOf(id.toString()))
                .collect(Collectors.toList());
    }
}