// src/main/java/com/cinema/testcinema/controller/UserProfileController.java
package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.UserProfileDto;
import com.cinema.testcinema.service.UserProfileService;
import com.cinema.testcinema.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{username}/profile")
public class UserProfileController {
    private final UserService users;
    private final UserProfileService profiles;

    public UserProfileController(UserService users, UserProfileService profiles) {
        this.users = users; this.profiles = profiles;
    }

    @GetMapping
    @PreAuthorize("#username == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<UserProfileDto> get(@PathVariable String username) {
        Long uid = users.findIdByUsername(username);
        return ResponseEntity.ok(profiles.getOrCreate(uid, username));
    }

    @PutMapping
    @PreAuthorize("#username == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<UserProfileDto> put(@PathVariable String username, @RequestBody UserProfileDto dto) {
        Long uid = users.findIdByUsername(username);
        dto.userId = uid;
        return ResponseEntity.ok(profiles.update(uid, dto));
    }
}
