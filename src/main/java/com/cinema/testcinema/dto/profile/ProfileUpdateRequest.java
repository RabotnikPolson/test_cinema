package com.cinema.testcinema.dto.profile;

public record ProfileUpdateRequest(
        String avatarUrl,
        Boolean isPrivate,
        String bio
) {
}
