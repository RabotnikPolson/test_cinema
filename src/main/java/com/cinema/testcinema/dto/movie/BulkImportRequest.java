package com.cinema.testcinema.dto.movie;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BulkImportRequest(
        @NotEmpty(message = "List of IDs cannot be empty")
        @Size(max = 50, message = "Max batch size is 50")
        List<@NotBlank(message = "ID cannot be blank") String> kinopoiskIds
) {
}
