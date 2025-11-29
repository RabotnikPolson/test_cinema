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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class UserProfileServiceTest {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.update("DELETE FROM user_profiles");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void emailChangeBlockedWithinSevenDays() {
        User user = createUser("limit@test.local", "limit-user");
        UserProfile profile = userProfileService.ensureProfile(user);
        profile.setLastProfileEditAt(Instant.now().minus(Duration.ofDays(1)));
        userProfileRepository.save(profile);

        ProfileUpdateRequest request = new ProfileUpdateRequest(null, "new-email@test.local", null, null);

        assertThatThrownBy(() -> userProfileService.updateProfile(user.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("7 дней");
    }

    @Test
    void emailChangeSyncsUserAndProfileAndUpdatesTimestamp() {
        User user = createUser("sync@test.local", "sync-user");
        userProfileService.ensureProfile(user);

        ProfileUpdateRequest request = new ProfileUpdateRequest(null, "updated@test.local", null, null);

        userProfileService.updateProfile(user.getId(), request);

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(updatedUser.getEmail()).isEqualTo("updated@test.local");
        assertThat(profile.getLastProfileEditAt()).isNotNull();
    }

    @Test
    void avatarChangeDoesNotUpdateTimestamp() {
        User user = createUser("avatar@test.local", "avatar-user");
        UserProfile profile = userProfileService.ensureProfile(user);
        Instant baseline = Instant.now().minus(Duration.ofDays(3));
        profile.setLastProfileEditAt(baseline);
        userProfileRepository.save(profile);

        ProfileUpdateRequest request = new ProfileUpdateRequest("http://avatar.local/img.png", null, null, null);

        userProfileService.updateProfile(user.getId(), request);

        UserProfile updated = userProfileRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(updated.getAvatarUrl()).isEqualTo("http://avatar.local/img.png");
        assertThat(updated.getLastProfileEditAt()).isEqualTo(baseline);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getEmail()).isEqualTo("avatar@test.local");
    }

    @Test
    void bioChangeDoesNotAffectEditWindow() {
        User user = createUser("bio@test.local", "bio-user");
        UserProfile profile = userProfileService.ensureProfile(user);
        profile.setLastProfileEditAt(Instant.now());
        userProfileRepository.save(profile);

        ProfileUpdateRequest request = new ProfileUpdateRequest(null, null, null, "New bio");

        userProfileService.updateProfile(user.getId(), request);

        UserProfile updated = userProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(updated.getBio()).isEqualTo("New bio");
        assertThat(updated.getLastProfileEditAt()).isEqualTo(profile.getLastProfileEditAt());
    }

    private User createUser(String email, String username) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("password"));
        return userRepository.save(user);
    }
}
