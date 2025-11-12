package com.cinema.testcinema.dto.review;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ReviewUpdateRequest", description = "Запрос на обновление текста отзыва")
public record ReviewUpdateRequest(
        @Schema(description = "Новый текст отзыва", example = "Спустя время впечатления только усилились")
        @NotBlank(message = "content must not be blank")
        @Size(min = 1, max = 5000, message = "content length must be between 1 and 5000 characters")
        String content
) {
}
