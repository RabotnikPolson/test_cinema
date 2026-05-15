package com.cinema.testcinema.dto.settings;

public record UserSettingsDto(
        String theme,
        String language,
        String email
) {
}
