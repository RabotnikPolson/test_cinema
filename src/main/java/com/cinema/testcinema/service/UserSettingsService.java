package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.settings.UserSettingsDto;
import com.cinema.testcinema.dto.settings.UserSettingsUpdateRequest;
import com.cinema.testcinema.exception.BusinessException;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.model.UserSettings;
import com.cinema.testcinema.repository.UserRepository;
import com.cinema.testcinema.repository.UserSettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Service
public class UserSettingsService {

    private static final Duration EMAIL_EDIT_WINDOW = Duration.ofDays(7);
    private static final Set<String> ALLOWED_THEMES = Set.of("LIGHT", "DARK");
    private static final Set<String> ALLOWED_LANGUAGES = Set.of("ru", "en", "kk");

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;

    public UserSettingsService(UserRepository userRepository, UserSettingsRepository userSettingsRepository) {
        this.userRepository = userRepository;
        this.userSettingsRepository = userSettingsRepository;
    }

    @Transactional
    public UserSettings ensureSettings(User user) {
        return userSettingsRepository.findByUserId(user.getId()).orElseGet(() -> {
            UserSettings settings = new UserSettings();
            settings.setUser(user);
            return userSettingsRepository.save(settings);
        });
    }

    @Transactional
    public UserSettingsDto getSettingsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
        UserSettings settings = ensureSettings(user);
        return toDto(user, settings);
    }

    @Transactional
    public UserSettingsDto updateSettingsForCurrentUser(Long userId, UserSettingsUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
        UserSettings settings = ensureSettings(user);

        if (request.theme() != null) {
            String normalizedTheme = request.theme().toUpperCase();
            if (!ALLOWED_THEMES.contains(normalizedTheme)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Недопустимая тема");
            }
            settings.setTheme(normalizedTheme);
        }

        if (request.language() != null) {
            String normalizedLanguage = request.language().toLowerCase();
            if (!ALLOWED_LANGUAGES.contains(normalizedLanguage)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "Недопустимый язык");
            }
            settings.setLanguage(normalizedLanguage);
        }

        if (request.email() != null) {
            handleEmailChange(request.email(), user, settings);
        }

        UserSettings savedSettings = userSettingsRepository.save(settings);
        return toDto(user, savedSettings);
    }

    private void handleEmailChange(String newEmail, User user, UserSettings settings) {
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            return;
        }

        Instant lastEmailEditAt = settings.getLastEmailEditAt();
        Instant now = Instant.now();
        if (lastEmailEditAt != null && lastEmailEditAt.plus(EMAIL_EDIT_WINDOW).isAfter(now)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Изменять email можно раз в 7 дней");
        }

        if (userRepository.existsByEmailIgnoreCase(newEmail)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Email уже зарегистрирован");
        }

        user.setEmail(newEmail);
        userRepository.save(user);
        settings.setLastEmailEditAt(now);
    }

    private UserSettingsDto toDto(User user, UserSettings settings) {
        return new UserSettingsDto(
                settings.getTheme(),
                settings.getLanguage(),
                user.getEmail()
        );
    }
}
