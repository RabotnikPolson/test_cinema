package com.cinema.testcinema.dto;

import java.time.Instant;
import java.util.UUID;

public record WatchBeatDto(
        String sessionId,
        Long movieId,
        Integer deltaSec,
        Integer currentPositionSec
) {}
