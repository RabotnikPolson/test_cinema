package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.AnalyticsSummaryDto;
import com.cinema.testcinema.dto.WatchBeatDto;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.repository.UserRepository;
import com.cinema.testcinema.security.AuthenticatedUserService;
import com.cinema.testcinema.service.WatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("")
public class WatchController {
    private final WatchService watchService;
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public WatchController(WatchService watchService,
                          UserRepository userRepository,
                          AuthenticatedUserService authenticatedUserService) {
        this.watchService = watchService;
        this.userRepository = userRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @PostMapping("/watch-history/beat")
    public ResponseEntity<Void> beat(@RequestBody WatchBeatDto dto, Authentication authentication) {
        Long userId = authenticatedUserService.requireCurrentUserId(authentication);
        User user = loadUser(userId);
        watchService.beat(user, dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/analytics/me/summary")
    public ResponseEntity<AnalyticsSummaryDto> mySummary(Authentication authentication) {
        Long userId = authenticatedUserService.requireCurrentUserId(authentication);
        return ResponseEntity.ok(watchService.mySummary(userId));
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
    }
}
