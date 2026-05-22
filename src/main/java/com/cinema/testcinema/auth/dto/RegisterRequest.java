package com.cinema.testcinema.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email(message = "Некорректный email")
        @NotBlank(message = "Email обязателен")
        String email,

        @NotBlank(message = "Имя пользователя обязательно")
        @Size(min = 2, max = 50, message = "Имя пользователя должно быть от 2 до 50 символов")
        String username,

        @NotBlank(message = "Пароль обязателен")
        @Size(min = 8, max = 255, message = "Пароль должен быть не короче 8 символов")
        String password
) {}
