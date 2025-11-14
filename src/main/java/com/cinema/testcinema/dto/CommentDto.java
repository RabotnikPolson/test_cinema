package com.cinema.testcinema.dto;

import java.time.Instant;

public record CommentDto(
        Long id,
        String imdbId,
        Long movieId,
        Long reviewId,
        Long parentId,
        Long userId,
        String username,
        String body,
        long likes,
        long dislikes,
        Instant createdAt
) {}
