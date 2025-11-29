package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.profile.ProfileMeDto;
import com.cinema.testcinema.dto.profile.ProfileUpdateRequest;
import com.cinema.testcinema.dto.profile.PublicProfileDto;
import com.cinema.testcinema.exception.BusinessException;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.model.UserProfile;
import com.cinema.testcinema.repository.UserProfileRepository;
import com.cinema.testcinema.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class UserProfileService {

    private static final Duration EDIT_WINDOW = Duration.ofDays(7);

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserRepository userRepository,
                              UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public UserProfile ensureProfile(User user) {
        return userProfileRepository.findByUserId(user.getId()).orElseGet(() -> {
            UserProfile profile = new UserProfile();
            profile.setUser(user);
            return userProfileRepository.save(profile);
        });
    }

    @Transactional
    public ProfileMeDto getOwnProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
        UserProfile profile = ensureProfile(user);
        return toOwnerDto(user, profile);
    }

    @Transactional
    public UserProfile getProfileByUsername(String username) {
        return userProfileRepository.findByUserUsername(username)
                .orElseGet(() -> {
                    User user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Профиль не найден"));
                    return ensureProfile(user);
                });
    }

    @Transactional
    public ProfileMeDto updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
        UserProfile profile = ensureProfile(user);

        boolean emailChanged = request.email() != null && !request.email().equalsIgnoreCase(user.getEmail());
        boolean requiresEditWindowCheck = emailChanged;

        if (requiresEditWindowCheck && !canEditEmail(profile)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Изменять никнейм или email можно раз в 7 дней");
        }

        if (request.avatarUrl() != null) {
            profile.setAvatarUrl(request.avatarUrl());
        }

        if (request.isPrivate() != null) {
            profile.setPrivate(request.isPrivate());
        }

        if (emailChanged) {
            if (userRepository.existsByEmail(request.email()) && !request.email().equalsIgnoreCase(user.getEmail())) {
                throw new BusinessException(HttpStatus.CONFLICT, "Email уже зарегистрирован");
            }
            user.setEmail(request.email());
        }

        if (request.bio() != null) {
            profile.setBio(request.bio());
        }

        if (requiresEditWindowCheck) {
            profile.setLastProfileEditAt(Instant.now());
        }

        UserProfile saved = userProfileRepository.save(profile);
        return toOwnerDto(user, saved);
    }

    public ProfileMeDto toOwnerDto(User user, UserProfile profile) {
        return new ProfileMeDto(
                user.getUsername(),
                user.getEmail(),
                profile.getAvatarUrl(),
                profile.isPrivate(),
                user.getCreatedAt(),
                profile.getBio()
        );
    }

    @Transactional(readOnly = true)
    public PublicProfileDto toPublicDto(User user, UserProfile profile) {
        return new PublicProfileDto(
                user.getUsername(),
                profile.getAvatarUrl(),
                user.getCreatedAt(),
                profile.isPrivate() ? null : profile.getBio()
        );
    }

    boolean canEditEmail(UserProfile profile) {
        Instant lastEdit = profile.getLastProfileEditAt();
        if (lastEdit == null) {
            return true;
        }
        return !lastEdit.plus(EDIT_WINDOW).isAfter(Instant.now());
    }
}
