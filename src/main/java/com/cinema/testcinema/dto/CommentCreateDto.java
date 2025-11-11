package com.cinema.testcinema.dto;

public record CommentCreateDto(
        String imdbId, Long reviewId, Long parentId, String body
) {}
