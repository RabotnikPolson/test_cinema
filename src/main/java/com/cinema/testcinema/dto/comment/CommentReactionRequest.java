package com.cinema.testcinema.dto.comment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CommentReactionRequest", description = "Запрос на добавление реакции к комментарию")
public record CommentReactionRequest(
        @Schema(description = "Эмодзи реакции", example = "👍")
        @NotBlank
        String emoji
) {
}
