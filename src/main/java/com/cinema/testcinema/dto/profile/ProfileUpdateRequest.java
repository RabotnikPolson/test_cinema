package com.cinema.testcinema.dto.profile;

import jakarta.validation.constraints.Email;

public record ProfileUpdateRequest(
        String avatarUrl,
        @Email(message = "Некорректный email")
        String email,
        Boolean isPrivate,
        String bio
) {
}
