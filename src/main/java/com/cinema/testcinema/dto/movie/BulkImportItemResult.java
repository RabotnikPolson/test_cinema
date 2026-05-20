package com.cinema.testcinema.dto.movie;

public record BulkImportItemResult(
        String kinopoiskId,
        String status,
        String title,
        Long movieId,
        String errorMessage
) {
}
