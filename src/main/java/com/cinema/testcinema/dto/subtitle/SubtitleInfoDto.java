package com.cinema.testcinema.dto.subtitle;

public record SubtitleInfoDto(
    String osFileId,
    String osSubtitleId,
    String language,
    String format,
    Double rating
) {}
