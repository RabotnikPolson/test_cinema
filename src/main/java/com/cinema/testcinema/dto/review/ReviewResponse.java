package com.cinema.testcinema.dto.review;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Schema(name = "ReviewResponse", description = "Отзыв с ограниченным списком ответов")
public class ReviewResponse {

    @Schema(description = "Идентификатор отзыва", example = "10")
    private Long id;

    @Schema(description = "Идентификатор пользователя", example = "100")
    private Long userId;

    @Schema(description = "Идентификатор фильма", example = "42")
    private Long movieId;

    @Schema(description = "Идентификатор родительского отзыва", example = "5", nullable = true)
    private Long parentId;

    @Schema(description = "Содержимое отзыва", example = "Очень атмосферное кино")
    private String content;

    @Schema(description = "Дата создания", example = "2024-05-12T10:15:30Z")
    private Instant createdAt;

    @Schema(description = "Дата последнего обновления", example = "2024-05-12T11:00:00Z")
    private Instant updatedAt;

    @Schema(description = "Был ли отзыв изменён")
    private boolean edited;

    @ArraySchema(arraySchema = @Schema(description = "Список прямых ответов"))
    private List<ReviewResponse> replies = new ArrayList<>();

    public ReviewResponse() {
    }

    public ReviewResponse(Long id, Long userId, Long movieId, Long parentId, String content,
                          Instant createdAt, Instant updatedAt, boolean edited) {
        this.id = id;
        this.userId = userId;
        this.movieId = movieId;
        this.parentId = parentId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.edited = edited;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getMovieId() {
        return movieId;
    }

    public Long getParentId() {
        return parentId;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isEdited() {
        return edited;
    }

    public List<ReviewResponse> getReplies() {
        return Collections.unmodifiableList(replies);
    }

    public void setReplies(List<ReviewResponse> replies) {
        this.replies = new ArrayList<>(replies);
    }
}
