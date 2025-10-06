package com.cinema.testcinema.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class RedisReviewCacheService {
    private final ConcurrentHashMap<Long, String> cache = new ConcurrentHashMap<>();

    public void saveReviewText(Long id, String text) { cache.put(id, text); }
    public void updateReviewCache(Long id, String text) { cache.put(id, text); }
    public void deleteReviewCache(Long id) { cache.remove(id); }
    public String getReviewText(Long id) { return cache.getOrDefault(id, "No cached review"); }
}
