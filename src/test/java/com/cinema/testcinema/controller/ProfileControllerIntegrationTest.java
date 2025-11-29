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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileControllerIntegrationTest {

    private static final long PUBLIC_USER_ID = 501L;
    private static final String PUBLIC_USERNAME = "public-user";
    private static final long PRIVATE_USER_ID = 502L;
    private static final String PRIVATE_USERNAME = "private-user";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM user_settings");
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id IN (?, ?)", PUBLIC_USER_ID, PRIVATE_USER_ID);
        jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", PUBLIC_USER_ID, PRIVATE_USER_ID);

        insertUserWithProfile(PUBLIC_USER_ID, "public@test.local", PUBLIC_USERNAME, false, "Public bio");
        insertUserWithProfile(PRIVATE_USER_ID, "private@test.local", PRIVATE_USERNAME, true, "Private bio");
    }

    @Test
    @DisplayName("Открытый профиль доступен без авторизации")
    void publicProfileAccessibleAnonymously() throws Exception {
        mockMvc.perform(get("/profile/" + PUBLIC_USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(PUBLIC_USERNAME))
                .andExpect(jsonPath("$.bio").value("Public bio"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    @DisplayName("Закрытый профиль возвращает только публичные данные")
    void privateProfileHidesSensitiveInfo() throws Exception {
        mockMvc.perform(get("/profile/" + PRIVATE_USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.username").value(PRIVATE_USERNAME))
                .andExpect(jsonPath("$.bio").value(nullValue()));
    }

    @Test
    @DisplayName("Изменения аватара и приватности не блокируются по времени")
    void avatarAndPrivacyChangesBypassEditWindow() throws Exception {
        Instant lockedEdit = Instant.now();
        jdbcTemplate.update("UPDATE user_profiles SET last_profile_edit_at = ? WHERE user_id = ?", lockedEdit, PRIVATE_USER_ID);

        String updatePayload = objectMapper.writeValueAsString(
                new UpdatePayload("http://img.local/new.png", true, null)
        );

        mockMvc.perform(put("/profile/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(PRIVATE_USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("http://img.local/new.png"))
                .andExpect(jsonPath("$.isPrivate").value(true));

        Instant lastEdit = jdbcTemplate.queryForObject(
                "SELECT last_profile_edit_at FROM user_profiles WHERE user_id = ?",
                Instant.class,
                PRIVATE_USER_ID
        );
        assertThat(lastEdit).isEqualTo(lockedEdit);
    }

    @Test
    @DisplayName("Bio можно менять без ограничений по времени")
    void bioChangesAreUnlimited() throws Exception {
        Instant lockedEdit = Instant.now();
        jdbcTemplate.update("UPDATE user_profiles SET last_profile_edit_at = ? WHERE user_id = ?", lockedEdit, PUBLIC_USER_ID);

        String payload = objectMapper.writeValueAsString(
                new UpdatePayload(null, null, "Updated bio")
        );

        mockMvc.perform(put("/profile/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(PUBLIC_USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Updated bio"));

        Instant lastEdit = jdbcTemplate.queryForObject(
                "SELECT last_profile_edit_at FROM user_profiles WHERE user_id = ?",
                Instant.class,
                PUBLIC_USER_ID
        );

        assertThat(lastEdit).isEqualTo(lockedEdit);
    }

    @Test
    @DisplayName("Email меняется только через настройки, профиль его не трогает")
    void profileUpdatesDoNotAffectEmail() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new UpdatePayload(null, true, "New bio")
        );

        mockMvc.perform(put("/profile/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(PUBLIC_USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("public@test.local"));

        String userEmail = jdbcTemplate.queryForObject("SELECT email FROM users WHERE id = ?", String.class, PUBLIC_USER_ID);
        assertThat(userEmail).isEqualTo("public@test.local");
    }

    @Test
    @DisplayName("Email, измененный через настройки, отображается в профиле")
    void profileReflectsSettingsEmailChange() throws Exception {
        String settingsPayload = objectMapper.writeValueAsString(new SettingsUpdate("DARK", "en", "updated@test.local"));

        mockMvc.perform(put("/settings/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(PUBLIC_USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settingsPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@test.local"));

        mockMvc.perform(get("/profile/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(PUBLIC_USER_ID)).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@test.local"))
                .andExpect(jsonPath("$.username").value(PUBLIC_USERNAME));
    }

    private void insertUserWithProfile(long userId, String email, String username, boolean isPrivate, String bio) {
        jdbcTemplate.update("INSERT INTO users (id, email, username, password_hash, created_at, enabled) VALUES (?, ?, ?, ?, ?, TRUE)",
                userId, email, username, "$2a$10$abcdefghijklmnopqrstuv", Instant.now());
        jdbcTemplate.update("INSERT INTO user_profiles (user_id, avatar_url, is_private, bio) VALUES (?, ?, ?, ?)",
                userId, null, isPrivate, bio);
    }

    private record UpdatePayload(String avatarUrl, Boolean isPrivate, String bio) {
    }

    private record SettingsUpdate(String theme, String language, String email) {
    }
}
