package com.cinema.testcinema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SettingsControllerIntegrationTest {

    private static final long USER_ID = 601L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM user_settings");
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", USER_ID);

        jdbcTemplate.update("INSERT INTO users (id, email, username, password_hash, created_at, enabled) VALUES (?, ?, ?, ?, ?, TRUE)",
                USER_ID, "settings@test.local", "settings-user", "$2a$10$abcdefghijklmnopqrstuv", Instant.now());
    }

    @Test
    @DisplayName("GET /settings/me создаёт настройки по умолчанию")
    void getSettingsCreatesDefaults() throws Exception {
        mockMvc.perform(get("/settings/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(USER_ID)).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("LIGHT"))
                .andExpect(jsonPath("$.language").value("ru"))
                .andExpect(jsonPath("$.email").value("settings@test.local"));

        String theme = jdbcTemplate.queryForObject("SELECT theme FROM user_settings WHERE user_id = ?", String.class, USER_ID);
        String language = jdbcTemplate.queryForObject("SELECT language FROM user_settings WHERE user_id = ?", String.class, USER_ID);
        Instant lastEmailEdit = jdbcTemplate.queryForObject("SELECT last_email_edit_at FROM user_settings WHERE user_id = ?", Instant.class, USER_ID);

        assertThat(theme).isEqualTo("LIGHT");
        assertThat(language).isEqualTo("ru");
        assertThat(lastEmailEdit).isNull();
    }

    @Test
    @DisplayName("Изменение темы и языка не влияет на last_email_edit_at")
    void themeAndLanguageUpdatesDoNotTouchEmailTimestamp() throws Exception {
        Instant baseline = Instant.now().minusSeconds(3600).truncatedTo(ChronoUnit.MILLIS);
        jdbcTemplate.update("INSERT INTO user_settings (user_id, theme, language, last_email_edit_at) VALUES (?, 'LIGHT', 'ru', ?)",
                USER_ID, baseline);

        String payload = objectMapper.writeValueAsString(new SettingsUpdate("DARK", "en", null));

        mockMvc.perform(put("/settings/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("DARK"))
                .andExpect(jsonPath("$.language").value("en"))
                .andExpect(jsonPath("$.email").value("settings@test.local"));

        Instant lastEmailEdit = jdbcTemplate.queryForObject("SELECT last_email_edit_at FROM user_settings WHERE user_id = ?", Instant.class, USER_ID);
        assertThat(lastEmailEdit).isEqualTo(baseline);
    }

    @Test
    @DisplayName("Смена email через настройки подчиняется лимиту 7 дней")
    void emailChangeEnforcesWindowInSettings() throws Exception {
        mockMvc.perform(get("/settings/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(USER_ID)).roles("USER")))
                .andExpect(status().isOk());

        String firstPayload = objectMapper.writeValueAsString(new SettingsUpdate(null, null, "updated@test.local"));

        mockMvc.perform(put("/settings/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@test.local"));

        String userEmail = jdbcTemplate.queryForObject("SELECT email FROM users WHERE id = ?", String.class, USER_ID);
        Instant lastEmailEdit = jdbcTemplate.queryForObject("SELECT last_email_edit_at FROM user_settings WHERE user_id = ?", Instant.class, USER_ID);

        assertThat(userEmail).isEqualTo("updated@test.local");
        assertThat(lastEmailEdit).isNotNull();

        String secondPayload = objectMapper.writeValueAsString(new SettingsUpdate(null, null, "second@test.local"));

        mockMvc.perform(put("/settings/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Изменять никнейм или email можно раз в 7 дней"));
    }

    private record SettingsUpdate(String theme, String language, String email) {
    }
}
