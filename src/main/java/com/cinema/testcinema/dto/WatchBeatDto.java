package com.cinema.testcinema.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record WatchBeatDto(
        @NotNull Long movieId,
        @NotNull UUID sessionId,
        int deltaSec,
        Instant clientTs,
        boolean paused
) {}
