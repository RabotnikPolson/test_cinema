package com.cinema.testcinema.dto.profile;

import java.util.List;

public record PublicProfileWithRatingsDto(
        PublicProfileDto profile,
        List<ProfileRatingDto> ratings,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
