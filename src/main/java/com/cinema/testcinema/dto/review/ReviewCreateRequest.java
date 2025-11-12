package com.cinema.testcinema.dto.review;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "ReviewCreateRequest", description = "Запрос на создание отзыва или ответа")
public record ReviewCreateRequest(
        @Schema(description = "Идентификатор фильма", example = "42")
        @NotNull(message = "movieId is required")
        Long movieId,

        @Schema(description = "Текст отзыва", example = "Отличный фильм!")
        @NotBlank(message = "content must not be blank")
        @Size(min = 1, max = 5000, message = "content length must be between 1 and 5000 characters")
        String content,

        @Schema(description = "Идентификатор родительского отзыва", example = "100", nullable = true)
        Long parentId
) {
}
