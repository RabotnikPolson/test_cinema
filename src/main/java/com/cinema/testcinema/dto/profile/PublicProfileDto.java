package com.cinema.testcinema.dto.profile;

import java.time.Instant;

public record PublicProfileDto(
        String nickname,
        String avatarUrl,
        Instant memberSince
) {
}
