// src/main/java/com/cinema/testcinema/dto/UserProfileDto.java
package com.cinema.testcinema.dto;

import java.util.List;

public class UserProfileDto {
    private Long userId;
    private String displayName;
    private String email;
    private String avatarUrl;
    private List<String> favoriteGenres;

    public UserProfileDto() { }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public List<String> getFavoriteGenres() { return favoriteGenres; }
    public void setFavoriteGenres(List<String> favoriteGenres) { this.favoriteGenres = favoriteGenres; }
}
