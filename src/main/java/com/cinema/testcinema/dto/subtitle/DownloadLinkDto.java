package com.cinema.testcinema.dto.subtitle;

public record DownloadLinkDto(
    String link,
    Integer remainingRequests
) {}
