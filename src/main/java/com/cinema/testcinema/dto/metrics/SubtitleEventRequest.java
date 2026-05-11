package com.cinema.testcinema.dto.metrics;

import com.cinema.testcinema.model.SubtitleEvent.SubtitleAction;
import jakarta.validation.constraints.NotNull;

public record SubtitleEventRequest(
        @NotNull(message = "movieId обязателен") Long movieId,
        @NotNull(message = "action обязателен") SubtitleAction action,
        String lang,
        String guestSessionId
) {}
