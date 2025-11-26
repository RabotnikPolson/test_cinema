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
            profile.setEmail(user.getEmail());
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

        boolean nicknameChanged = request.nickname() != null && !request.nickname().equals(profile.getNickname());
        String currentEmail = profile.getEmail();
        boolean emailChanged = request.email() != null && (currentEmail == null || !request.email().equalsIgnoreCase(currentEmail));
        boolean requiresEditWindowCheck = nicknameChanged || emailChanged;

        if (requiresEditWindowCheck && !canEditNicknameOrEmail(profile)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Изменять никнейм или email можно раз в 7 дней");
        }

        if (nicknameChanged) {
            if (userProfileRepository.existsByNickname(request.nickname()) && request.nickname() != null) {
                throw new BusinessException(HttpStatus.CONFLICT, "Никнейм уже используется");
            }
            profile.setNickname(request.nickname());
        }

        if (request.avatarUrl() != null) {
            profile.setAvatarUrl(request.avatarUrl());
        }

        if (request.isPrivate() != null) {
            profile.setPrivate(request.isPrivate());
        }

        if (emailChanged) {
            if ((userRepository.existsByEmail(request.email()) && !request.email().equalsIgnoreCase(user.getEmail()))
                    || (userProfileRepository.existsByEmail(request.email()) && (currentEmail == null || !request.email().equalsIgnoreCase(currentEmail)))) {
                throw new BusinessException(HttpStatus.CONFLICT, "Email уже зарегистрирован");
            }
            user.setEmail(request.email());
            profile.setEmail(request.email());
        }

        if (requiresEditWindowCheck) {
            profile.setLastProfileEditAt(Instant.now());
        }

        UserProfile saved = userProfileRepository.save(profile);
        return toOwnerDto(user, saved);
    }

    public ProfileMeDto toOwnerDto(User user, UserProfile profile) {
        return new ProfileMeDto(
                profile.getNickname(),
                profile.getEmail(),
                profile.getAvatarUrl(),
                profile.isPrivate(),
                user.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public PublicProfileDto toPublicDto(User user, UserProfile profile) {
        return new PublicProfileDto(
                profile.getNickname(),
                profile.getAvatarUrl(),
                user.getCreatedAt()
        );
    }

    boolean canEditNicknameOrEmail(UserProfile profile) {
        Instant lastEdit = profile.getLastProfileEditAt();
        if (lastEdit == null) {
            return true;
        }
        return !lastEdit.plus(EDIT_WINDOW).isAfter(Instant.now());
    }
}
