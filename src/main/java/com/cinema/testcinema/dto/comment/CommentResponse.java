package com.cinema.testcinema.dto.comment;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Schema(name = "CommentResponse", description = "Комментарий с прямыми ответами")
public class CommentResponse {

    @Schema(description = "Идентификатор комментария", example = "10")
    private Long id;

    @Schema(description = "Идентификатор пользователя", example = "100")
    private Long userId;

    @Schema(description = "Идентификатор фильма", example = "42")
    private Long movieId;

    @Schema(description = "Идентификатор родительского комментария", example = "5", nullable = true)
    private Long parentId;

    @Schema(description = "Текст комментария", example = "Согласен")
    private String content;

    @Schema(description = "Дата создания", example = "2024-05-12T10:15:30Z")
    private Instant createdAt;

    @Schema(description = "Дата обновления", example = "2024-05-12T10:20:30Z")
    private Instant updatedAt;

    @Schema(description = "Был ли комментарий отредактирован")
    private boolean edited;

    @ArraySchema(arraySchema = @Schema(description = "Список прямых ответов"))
    private List<CommentResponse> replies = new ArrayList<>();

    @ArraySchema(arraySchema = @Schema(description = "Сводка реакций по эмодзи"))
    private List<CommentReactionSummary> reactions = new ArrayList<>();

    public CommentResponse() {
    }

    public CommentResponse(Long id, Long userId, Long movieId, Long parentId, String content,
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

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public List<CommentResponse> getReplies() {
        return Collections.unmodifiableList(replies);
    }

    public void setReplies(List<CommentResponse> replies) {
        this.replies = new ArrayList<>(replies);
    }

    public List<CommentReactionSummary> getReactions() {
        return Collections.unmodifiableList(reactions);
    }

    public void setReactions(List<CommentReactionSummary> reactions) {
        this.reactions = new ArrayList<>(reactions);
    }
}
