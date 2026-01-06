package com.cinema.testcinema.dto.profile;

import java.time.Instant;

public record ProfileMeDto(
        String username,
        String email,
        String avatarUrl,
        boolean isPrivate,
        Instant memberSince,
        String bio
) {
}
