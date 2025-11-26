package com.cinema.testcinema.dto.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailChangeRequest(
        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный email")
        String email
) {
}
