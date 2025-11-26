package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.profile.ProfileUpdateRequest;
import com.cinema.testcinema.exception.BusinessException;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.model.UserProfile;
import com.cinema.testcinema.repository.UserProfileRepository;
import com.cinema.testcinema.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@ActiveProfiles("test")
class UserProfileServiceTest {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private EmailService emailService;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.update("DELETE FROM email_verification_tokens");
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void nicknameChangeBlockedWithinSevenDays() {
        User user = createUser("profile@test.local", "profile-user");
        UserProfile profile = userProfileService.ensureProfile(user);
        profile.setLastProfileEditAt(Instant.now().minus(Duration.ofDays(2)));
        userProfileRepository.save(profile);

        ProfileUpdateRequest request = new ProfileUpdateRequest("newNick", null, null, null);

        assertThatThrownBy(() -> userProfileService.updateProfile(user.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("7 дней");
    }

    @Test
    void emailVerificationSuccessFlow() {
        String originalEmail = "verify@test.local";
        User user = createUser(originalEmail, "verify-user");
        userProfileService.ensureProfile(user);

        AtomicReference<String> codeRef = new AtomicReference<>();
        doAnswer(invocation -> {
            codeRef.set(invocation.getArgument(1));
            return null;
        }).when(emailService).sendEmailVerificationCode(anyString(), anyString());

        emailVerificationService.requestEmailChange(user, "new-email@test.local");
        UserProfile pendingProfile = userProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(pendingProfile.isEmailVerified()).isFalse();
        assertThat(pendingProfile.getLastProfileEditAt()).isNull();
        assertThat(pendingProfile.getEmail()).isEqualTo(originalEmail);

        String code = codeRef.get();
        emailVerificationService.verifyCode(user, code);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(updated.getEmail()).isEqualTo("new-email@test.local");
        assertThat(profile.getEmail()).isEqualTo("new-email@test.local");
        assertThat(profile.isEmailVerified()).isTrue();
        assertThat(profile.getLastProfileEditAt()).isNotNull();
    }

    @Test
    void newProfileEmailIsUnverifiedByDefault() {
        User user = createUser("fresh@test.local", "fresh-user");

        UserProfile profile = userProfileService.ensureProfile(user);

        assertThat(profile.isEmailVerified()).isFalse();
        assertThat(profile.getLastProfileEditAt()).isNull();
    }

    @Test
    void updateProfileWithoutEmailDoesNotVerifyEmailOrUpdateTimestamp() {
        User user = createUser("avatar@test.local", "avatar-user");
        UserProfile profile = userProfileService.ensureProfile(user);

        ProfileUpdateRequest request = new ProfileUpdateRequest(null, "http://avatar.local/img.png", null, null);

        userProfileService.updateProfile(user.getId(), request);

        UserProfile updated = userProfileRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(updated.isEmailVerified()).isFalse();
        assertThat(updated.getLastProfileEditAt()).isNull();
        assertThat(updated.getEmail()).isEqualTo("avatar@test.local");
        assertThat(updated.getAvatarUrl()).isEqualTo("http://avatar.local/img.png");
    }

    @Test
    void emailVerificationFailsWithWrongCode() {
        User user = createUser("fail@test.local", "fail-user");
        userProfileService.ensureProfile(user);

        doAnswer(invocation -> null).when(emailService).sendEmailVerificationCode(anyString(), anyString());

        emailVerificationService.requestEmailChange(user, "fail-new@test.local");

        assertThatThrownBy(() -> emailVerificationService.verifyCode(user, "000000"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("код");
    }

    @Test
    void emailChangeRequestsAllowedUntilVerified() {
        User user = createUser("multi@test.local", "multi-user");
        userProfileService.ensureProfile(user);

        doAnswer(invocation -> null).when(emailService).sendEmailVerificationCode(anyString(), anyString());

        emailVerificationService.requestEmailChange(user, "first@test.local");
        emailVerificationService.requestEmailChange(user, "second@test.local");

        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(profile.getEmail()).isEqualTo("multi@test.local");
        assertThat(profile.isEmailVerified()).isFalse();
        assertThat(profile.getLastProfileEditAt()).isNull();
    }

    @Test
    void emailChangeBlockedAfterRecentVerifiedChange() {
        User user = createUser("limit@test.local", "limit-user");
        userProfileService.ensureProfile(user);

        AtomicReference<String> codeRef = new AtomicReference<>();
        doAnswer(invocation -> {
            codeRef.set(invocation.getArgument(1));
            return null;
        }).when(emailService).sendEmailVerificationCode(anyString(), anyString());

        emailVerificationService.requestEmailChange(user, "first-verified@test.local");
        String firstCode = codeRef.get();
        emailVerificationService.verifyCode(user, firstCode);

        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElseThrow();
        Instant lastEdit = profile.getLastProfileEditAt();
        assertThat(lastEdit).isNotNull();

        emailVerificationService.requestEmailChange(user, "second-try@test.local");
        String secondCode = codeRef.get();

        assertThatThrownBy(() -> emailVerificationService.verifyCode(user, secondCode))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("7 дней");

        UserProfile afterAttempt = userProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(afterAttempt.getLastProfileEditAt()).isEqualTo(lastEdit);
    }

    private User createUser(String email, String username) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("password"));
        return userRepository.save(user);
    }
}
