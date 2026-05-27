package com.cinema.testcinema.dto.metrics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecClickRequest(
        @NotNull(message = "movieId обязателен") Long movieId,
        @NotBlank(message = "strategy обязателен") String strategy
) {}
