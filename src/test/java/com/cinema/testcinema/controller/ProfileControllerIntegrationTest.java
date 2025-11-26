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
        jdbcTemplate.update("DELETE FROM email_verification_tokens");
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id IN (?, ?)", PUBLIC_USER_ID, PRIVATE_USER_ID);
        jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", PUBLIC_USER_ID, PRIVATE_USER_ID);

        insertUserWithProfile(PUBLIC_USER_ID, "public@test.local", PUBLIC_USERNAME, false);
        insertUserWithProfile(PRIVATE_USER_ID, "private@test.local", PRIVATE_USERNAME, true);
    }

    @Test
    @DisplayName("Открытый профиль доступен без авторизации")
    void publicProfileAccessibleAnonymously() throws Exception {
        mockMvc.perform(get("/profile/" + PUBLIC_USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("public@test.local"))
                .andExpect(jsonPath("$.nickname").value("public-nick"));
    }

    @Test
    @DisplayName("Закрытый профиль возвращает только публичные данные")
    void privateProfileHidesSensitiveInfo() throws Exception {
        mockMvc.perform(get("/profile/" + PRIVATE_USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.nickname").value("private-nick"));
    }

    @Test
    @DisplayName("Смена никнейма и email ограничена 7 днями")
    void nicknameAndEmailChangesRespectSevenDayLimit() throws Exception {
        String firstUpdate = objectMapper.writeValueAsString(
                new UpdatePayload("fresh-nick", null, "updated@test.local", null)
        );

        mockMvc.perform(put("/profile/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(PUBLIC_USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstUpdate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("fresh-nick"))
                .andExpect(jsonPath("$.email").value("updated@test.local"));

        Instant lastEdit = jdbcTemplate.queryForObject(
                "SELECT last_profile_edit_at FROM user_profiles WHERE user_id = ?",
                Instant.class,
                PUBLIC_USER_ID
        );
        assertThat(lastEdit).isNotNull();

        String secondUpdate = objectMapper.writeValueAsString(
                new UpdatePayload("second-nick", null, "second@test.local", null)
        );

        mockMvc.perform(put("/profile/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(PUBLIC_USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondUpdate))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Изменять никнейм или email можно раз в 7 дней"));

        mockMvc.perform(get("/profile/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(PUBLIC_USER_ID)).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@test.local"))
                .andExpect(jsonPath("$.nickname").value("fresh-nick"));
    }

    @Test
    @DisplayName("Изменения аватара и приватности не блокируются по времени")
    void avatarAndPrivacyChangesBypassEditWindow() throws Exception {
        Instant lockedEdit = Instant.now();
        jdbcTemplate.update("UPDATE user_profiles SET last_profile_edit_at = ? WHERE user_id = ?", lockedEdit, PRIVATE_USER_ID);

        String updatePayload = objectMapper.writeValueAsString(
                new UpdatePayload(null, "http://img.local/new.png", null, true)
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
    @DisplayName("Смена email через PUT /profile/me синхронизирует User и UserProfile")
    void emailChangeUpdatesUserAndProfile() throws Exception {
        String payload = objectMapper.writeValueAsString(
                new UpdatePayload(null, null, "sync@test.local", null)
        );

        mockMvc.perform(put("/profile/me")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(PUBLIC_USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("sync@test.local"));

        String userEmail = jdbcTemplate.queryForObject("SELECT email FROM users WHERE id = ?", String.class, PUBLIC_USER_ID);
        String profileEmail = jdbcTemplate.queryForObject("SELECT email FROM user_profiles WHERE user_id = ?", String.class, PUBLIC_USER_ID);
        Instant lastEdit = jdbcTemplate.queryForObject("SELECT last_profile_edit_at FROM user_profiles WHERE user_id = ?", Instant.class, PUBLIC_USER_ID);

        assertThat(userEmail).isEqualTo("sync@test.local");
        assertThat(profileEmail).isEqualTo("sync@test.local");
        assertThat(lastEdit).isNotNull();
    }

    private void insertUserWithProfile(long userId, String email, String username, boolean isPrivate) {
        jdbcTemplate.update("INSERT INTO users (id, email, username, password_hash, created_at, enabled) VALUES (?, ?, ?, ?, ?, TRUE)",
                userId, email, username, "$2a$10$abcdefghijklmnopqrstuv", Instant.now());
        jdbcTemplate.update("INSERT INTO user_profiles (user_id, nickname, email, email_verified, is_private) VALUES (?, ?, ?, FALSE, ?)",
                userId, username + "-nick", email, isPrivate);
    }

    private record UpdatePayload(String nickname, String avatarUrl, String email, Boolean isPrivate) {
    }
}
