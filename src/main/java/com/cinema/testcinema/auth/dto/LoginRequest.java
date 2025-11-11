package com.cinema.testcinema.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Email(message = "Некорректный email")
        @NotBlank(message = "Email обязателен")
        String email,

        @NotBlank(message = "Пароль обязателен")
        String password
) {}
