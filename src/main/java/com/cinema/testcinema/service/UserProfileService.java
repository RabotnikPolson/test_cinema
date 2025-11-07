// src/main/java/com/cinema/testcinema/service/UserProfileService.java
package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.UserProfileDto;
import com.cinema.testcinema.model.UserProfile;
import com.cinema.testcinema.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class UserProfileService {
    private final UserProfileRepository repo;

    public UserProfileService(UserProfileRepository repo) { this.repo = repo; }

    public UserProfileDto getOrCreate(Long userId, String usernameEmail) {
        var up = repo.findById(userId).orElseGet(() -> {
            var p = new UserProfile();
            p.setUserId(userId);
            p.setDisplayName(usernameEmail);
            p.setEmail(usernameEmail);
            p.setFavoriteGenres(new String[0]);
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            return repo.save(p);
        });
        return toDto(up);
    }

    public UserProfileDto update(Long userId, UserProfileDto dto) {
        var up = repo.findById(userId).orElseThrow();
        up.setDisplayName(dto.displayName);
        up.setEmail(dto.email);
        up.setAvatarUrl(dto.avatarUrl);
        up.setFavoriteGenres(dto.favoriteGenres == null ? null : dto.favoriteGenres.toArray(String[]::new));
        up.setUpdatedAt(Instant.now());
        return toDto(repo.save(up));
    }

    private static UserProfileDto toDto(UserProfile up) {
        var dto = new UserProfileDto();
        dto.userId = up.getUserId();
        dto.displayName = up.getDisplayName();
        dto.email = up.getEmail();
        dto.avatarUrl = up.getAvatarUrl();
        dto.favoriteGenres = up.getFavoriteGenres() == null ? List.of() : List.of(up.getFavoriteGenres());
        dto.createdAt = up.getCreatedAt();
        dto.updatedAt = up.getUpdatedAt();
        return dto;
    }
}
