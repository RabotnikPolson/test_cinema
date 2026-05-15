package com.cinema.testcinema.dto.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "CommentUpdateRequest", description = "Запрос на обновление комментария")
public record CommentUpdateRequest(
        @Schema(description = "Текст комментария", example = "Обновленный текст")
        @NotBlank
        @Size(max = 5000)
        String content
) {
}
