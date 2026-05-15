package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.settings.UserSettingsDto;
import com.cinema.testcinema.dto.settings.UserSettingsUpdateRequest;
import com.cinema.testcinema.security.AuthenticatedUserService;
import com.cinema.testcinema.service.UserSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    private final UserSettingsService userSettingsService;
    private final AuthenticatedUserService authenticatedUserService;

    public SettingsController(UserSettingsService userSettingsService,
                              AuthenticatedUserService authenticatedUserService) {
        this.userSettingsService = userSettingsService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping("/me")
    public UserSettingsDto getSettings(Authentication authentication) {
        Long userId = authenticatedUserService.requireCurrentUserId(authentication);
        return userSettingsService.getSettingsForUser(userId);
    }

    @PutMapping("/me")
    public UserSettingsDto updateSettings(@Valid @RequestBody UserSettingsUpdateRequest request,
                                          Authentication authentication) {
        Long userId = authenticatedUserService.requireCurrentUserId(authentication);
        return userSettingsService.updateSettingsForCurrentUser(userId, request);
    }
}
