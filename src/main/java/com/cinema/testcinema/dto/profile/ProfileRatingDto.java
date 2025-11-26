package com.cinema.testcinema.dto.profile;

public record ProfileRatingDto(
        Long movieId,
        String title,
        String posterUrl,
        String imdbId,
        Short score,
        String reviewText
) {
}
