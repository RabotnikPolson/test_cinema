package com.cinema.testcinema.dto.profile;

import java.time.Instant;

public record PublicProfileDto(
        String username,
        String avatarUrl,
        Instant memberSince,
        String bio
) {
}
