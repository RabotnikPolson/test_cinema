package com.cinema.testcinema.dto.metrics;

import jakarta.validation.constraints.NotNull;

public record MovieClickRequest(
        @NotNull(message = "movieId обязателен") Long movieId,
        String guestSessionId
) {}
