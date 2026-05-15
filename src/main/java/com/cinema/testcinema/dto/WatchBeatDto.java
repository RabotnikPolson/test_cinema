package com.cinema.testcinema.dto;

import java.time.Instant;
import java.util.UUID;

public record WatchBeatDto(Long movieId, UUID sessionId, int deltaSec, Instant clientTs, boolean paused) {}
