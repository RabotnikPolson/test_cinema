package com.cinema.testcinema.dto;

import java.time.Instant;

public record ReviewDto(
        Long id, String imdbId, Long movieId, Long userId, String username,
        String body, Instant createdAt
) {}
