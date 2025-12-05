package com.cinema.testcinema.dto.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "CommentCreateRequest", description = "Запрос на создание комментария")
public record CommentCreateRequest(
        @Schema(description = "Идентификатор фильма", example = "42")
        @NotNull
        Long movieId,

        @Schema(description = "Текст комментария", example = "Мне понравилось")
        @NotBlank
        @Size(max = 5000)
        String content,

        @Schema(description = "Идентификатор родительского комментария", example = "10", nullable = true)
        Long parentId
) {
}
