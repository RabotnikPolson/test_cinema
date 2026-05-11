package com.cinema.testcinema.dto.metrics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SearchLogRequest(
        @NotBlank(message = "query не может быть пустым")
        @Size(max = 255, message = "query не длиннее 255 символов")
        String query,

        @NotNull(message = "resultCount обязателен")
        Integer resultCount,
        
        String guestSessionId
) {}
