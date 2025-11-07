// src/main/java/com/cinema/testcinema/controller/SubscriptionController.java
package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.SubscriptionDto;
import com.cinema.testcinema.service.SubscriptionService;
import com.cinema.testcinema.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/{username}/subscription")
public class SubscriptionController {
    private final UserService users;
    private final SubscriptionService subs;

    public SubscriptionController(UserService users, SubscriptionService subs) {
        this.users = users; this.subs = subs;
    }

    @GetMapping
    @PreAuthorize("#username == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<SubscriptionDto> get(@PathVariable String username) {
        Long uid = users.findIdByUsername(username);
        return ResponseEntity.ok(subs.getOrDefault(uid));
    }

    @PostMapping("/pro")
    @PreAuthorize("#username == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<SubscriptionDto> subscribe(@PathVariable String username) {
        Long uid = users.findIdByUsername(username);
        return ResponseEntity.ok(subs.subscribePro(uid));
    }

    @PostMapping("/cancel")
    @PreAuthorize("#username == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<SubscriptionDto> cancel(@PathVariable String username) {
        Long uid = users.findIdByUsername(username);
        return ResponseEntity.ok(subs.cancel(uid));
    }
}
