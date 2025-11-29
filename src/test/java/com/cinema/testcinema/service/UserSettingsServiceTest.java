package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.settings.UserSettingsDto;
import com.cinema.testcinema.dto.settings.UserSettingsUpdateRequest;
import com.cinema.testcinema.exception.BusinessException;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.model.UserSettings;
import com.cinema.testcinema.repository.UserRepository;
import com.cinema.testcinema.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class UserSettingsServiceTest {

    @Autowired
    private UserSettingsService userSettingsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.update("DELETE FROM user_settings");
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void defaultsCreatedOnFirstAccess() {
        User user = createUser("default@test.local", "default-user");

        UserSettingsDto dto = userSettingsService.getSettingsForUser(user.getId());

        assertThat(dto.theme()).isEqualTo("LIGHT");
        assertThat(dto.language()).isEqualTo("ru");
        assertThat(dto.email()).isEqualTo("default@test.local");

        UserSettings settings = userSettingsRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(settings.getLastEmailEditAt()).isNull();
    }

    @Test
    void themeAndLanguageUpdateKeepsEmailTimestamp() {
        User user = createUser("update@test.local", "update-user");
        UserSettings settings = userSettingsService.ensureSettings(user);
        Instant baseline = Instant.now().minusSeconds(7200).truncatedTo(ChronoUnit.MILLIS);
        settings.setLastEmailEditAt(baseline);
        userSettingsRepository.save(settings);

        UserSettingsDto dto = userSettingsService.updateSettingsForCurrentUser(user.getId(),
                new UserSettingsUpdateRequest("DARK", "en", null));

        UserSettings reloaded = userSettingsRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(dto.theme()).isEqualTo("DARK");
        assertThat(dto.language()).isEqualTo("en");
        assertThat(dto.email()).isEqualTo("update@test.local");
        assertThat(reloaded.getLastEmailEditAt()).isEqualTo(baseline);
    }

    @Test
    void emailChangeRespectsCooldown() {
        User user = createUser("cooldown@test.local", "cooldown-user");
        userSettingsService.ensureSettings(user);

        UserSettingsDto firstUpdate = userSettingsService.updateSettingsForCurrentUser(user.getId(),
                new UserSettingsUpdateRequest(null, null, "updated@test.local"));

        assertThat(firstUpdate.email()).isEqualTo("updated@test.local");
        assertThat(userRepository.findById(user.getId()).orElseThrow().getEmail()).isEqualTo("updated@test.local");
        assertThat(userSettingsRepository.findByUserId(user.getId()).orElseThrow().getLastEmailEditAt()).isNotNull();

        assertThatThrownBy(() -> userSettingsService.updateSettingsForCurrentUser(user.getId(),
                        new UserSettingsUpdateRequest(null, null, "second@test.local")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("7 дней");
    }

    private User createUser(String email, String username) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("password"));
        return userRepository.save(user);
    }
}
