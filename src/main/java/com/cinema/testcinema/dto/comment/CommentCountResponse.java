package com.cinema.testcinema.dto.comment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CommentCountResponse", description = "Счётчики комментариев по фильму")
public class CommentCountResponse {

    @Schema(description = "Количество корневых комментариев", example = "123")
    private long rootCount;

    @Schema(description = "Общее количество комментариев (корневые + ответы)", example = "546")
    private long totalCount;

    public CommentCountResponse() {
    }

    public CommentCountResponse(long rootCount, long totalCount) {
        this.rootCount = rootCount;
        this.totalCount = totalCount;
    }

    public long getRootCount() {
        return rootCount;
    }

    public void setRootCount(long rootCount) {
        this.rootCount = rootCount;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }
}
