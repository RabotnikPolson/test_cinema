package com.cinema.testcinema.dto.settings;

import jakarta.validation.constraints.Email;

public record UserSettingsUpdateRequest(
        String theme,
        String language,
        @Email(message = "Некорректный email")
        String email
) {
}
