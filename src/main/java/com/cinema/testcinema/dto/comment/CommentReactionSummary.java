package com.cinema.testcinema.dto.comment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CommentReactionSummary", description = "Сводка реакций для комментария")
public record CommentReactionSummary(
        @Schema(description = "Эмодзи реакции", example = "👍")
        String emoji,
        @Schema(description = "Количество реакций", example = "5")
        long count
) {
}
