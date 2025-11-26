package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.profile.EmailChangeRequest;
import com.cinema.testcinema.dto.profile.EmailVerificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @MockBean
    private com.cinema.testcinema.service.EmailService emailService;

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
    @DisplayName("Смена и подтверждение email через контроллер")
    void changeAndVerifyEmailFlow() throws Exception {
        AtomicReference<String> codeRef = new AtomicReference<>();
        doAnswer(invocation -> {
            codeRef.set(invocation.getArgument(1));
            return null;
        }).when(emailService).sendEmailVerificationCode(anyString(), anyString());

        EmailChangeRequest changeRequest = new EmailChangeRequest("updated@test.local");
        mockMvc.perform(post("/profile/me/email/change")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(PUBLIC_USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("код отправлен на почту"));

        String code = codeRef.get();
        assertThat(code).isNotBlank();

        EmailVerificationRequest verifyRequest = new EmailVerificationRequest(code);
        mockMvc.perform(post("/profile/me/email/verify")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(PUBLIC_USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@test.local"))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    @DisplayName("Запрос смены email не ограничен до подтверждения")
    void emailChangeRequestsNotLimitedUntilVerified() throws Exception {
        doAnswer(invocation -> null).when(emailService).sendEmailVerificationCode(anyString(), anyString());

        EmailChangeRequest firstRequest = new EmailChangeRequest("first-controller@test.local");
        mockMvc.perform(post("/profile/me/email/change")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(PUBLIC_USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk());

        EmailChangeRequest secondRequest = new EmailChangeRequest("second-controller@test.local");
        mockMvc.perform(post("/profile/me/email/change")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(PUBLIC_USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Лимит 7 дней применяется после подтвержденной смены email")
    void emailChangeBlockedAfterVerificationWithinSevenDays() throws Exception {
        AtomicReference<String> codeRef = new AtomicReference<>();
        doAnswer(invocation -> {
            codeRef.set(invocation.getArgument(1));
            return null;
        }).when(emailService).sendEmailVerificationCode(anyString(), anyString());

        EmailChangeRequest changeRequest = new EmailChangeRequest("verify-controller@test.local");
        mockMvc.perform(post("/profile/me/email/change")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(PUBLIC_USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isOk());

        EmailVerificationRequest verifyRequest = new EmailVerificationRequest(codeRef.get());
        mockMvc.perform(post("/profile/me/email/verify")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(PUBLIC_USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk());

        EmailChangeRequest secondRequest = new EmailChangeRequest("second-try-controller@test.local");
        mockMvc.perform(post("/profile/me/email/change")
                        .with(SecurityMockMvcRequestPostProcessors.user(String.valueOf(PUBLIC_USER_ID)).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Изменять никнейм или email можно раз в 7 дней"));
    }

    private void insertUserWithProfile(long userId, String email, String username, boolean isPrivate) {
        jdbcTemplate.update("INSERT INTO users (id, email, username, password_hash, created_at, enabled) VALUES (?, ?, ?, ?, ?, TRUE)",
                userId, email, username, "$2a$10$abcdefghijklmnopqrstuv", Instant.now());
        jdbcTemplate.update("INSERT INTO user_profiles (user_id, nickname, email, email_verified, is_private) VALUES (?, ?, ?, TRUE, ?)",
                userId, username + "-nick", email, isPrivate);
    }
}
