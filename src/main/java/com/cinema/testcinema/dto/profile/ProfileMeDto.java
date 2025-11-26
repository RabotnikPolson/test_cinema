package com.cinema.testcinema.dto.profile;

import java.time.Instant;

public record ProfileMeDto(
        String nickname,
        String email,
        String avatarUrl,
        boolean isPrivate,
        Instant memberSince
) {
}
