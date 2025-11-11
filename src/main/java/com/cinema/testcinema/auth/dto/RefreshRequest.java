package com.cinema.testcinema.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "Refresh token обязателен")
        String refreshToken
) {}
