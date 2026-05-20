package com.cinema.testcinema.dto.movie;

import java.util.List;

public record BulkImportResponse(
        int total,
        int success,
        int failed,
        List<BulkImportItemResult> results
) {
}
