// src/main/java/com/cinema/testcinema/dto/UserProfileDto.java
package com.cinema.testcinema.dto;

import java.time.Instant;
import java.util.List;

public class UserProfileDto {
    public Long userId;
    public String displayName;
    public String email;
    public String avatarUrl;
    public List<String> favoriteGenres;
    public Instant createdAt;
    public Instant updatedAt;
}
