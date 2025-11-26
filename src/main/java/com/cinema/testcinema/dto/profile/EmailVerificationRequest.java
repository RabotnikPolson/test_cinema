package com.cinema.testcinema.dto.profile;

import jakarta.validation.constraints.NotBlank;

public record EmailVerificationRequest(
        @NotBlank(message = "Код обязателен")
        String code
) {
}
