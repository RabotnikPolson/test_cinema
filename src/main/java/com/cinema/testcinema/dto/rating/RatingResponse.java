package com.cinema.testcinema.dto.rating;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "RatingResponse", description = "Ответ с информацией о рейтинге фильма")
public record RatingResponse(
        @Schema(description = "Идентификатор рейтинга", example = "15")
        Long id,

        @Schema(description = "Идентификатор пользователя", example = "100")
        Long userId,

        @Schema(description = "Идентификатор фильма", example = "42")
        Long movieId,

        @Schema(description = "Оценка", example = "9")
        Short score,

        @Schema(description = "Дата создания рейтинга", example = "2024-05-12T10:15:30Z")
        Instant createdAt
) {
}
