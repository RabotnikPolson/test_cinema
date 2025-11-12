package com.cinema.testcinema.dto.rating;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(name = "RatingRequest", description = "Запрос на создание или обновление рейтинга фильма")
public record RatingRequest(
        @Schema(description = "Идентификатор фильма", example = "42")
        @NotNull(message = "movieId is required")
        Long movieId,

        @Schema(description = "Оценка пользователя", example = "8")
        @NotNull(message = "score is required")
        @Min(value = 1, message = "score must be between 1 and 10")
        @Max(value = 10, message = "score must be between 1 and 10")
        Short score
) {
}
