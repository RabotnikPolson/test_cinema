// src/main/java/com/cinema/testcinema/controller/UserSettingsController.java
package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.UserSettingsDto;
import com.cinema.testcinema.model.UserSettings;
import com.cinema.testcinema.service.UserService;
import com.cinema.testcinema.service.UserSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/{username}/settings")
public class UserSettingsController {

    private final UserSettingsService settingsService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public UserSettingsController(UserSettingsService settingsService, UserService userService, ObjectMapper objectMapper) {
        this.settingsService = settingsService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<UserSettingsDto> get(@PathVariable String username) {
        Long uid = userService.findIdByUsername(username);
        UserSettings s = settingsService.getOrCreate(uid);

        UserSettingsDto dto = new UserSettingsDto();
        dto.setUserId(s.getUserId());
        try {
            dto.setData(objectMapper.readValue(s.getData(), Map.class));
        } catch (Exception e) {
            dto.setData(Map.of());
        }
        return ResponseEntity.ok(dto);
    }

    @PutMapping
    public ResponseEntity<UserSettingsDto> update(@PathVariable String username, @RequestBody Map<String, Object> body) {
        Long uid = userService.findIdByUsername(username);
        try {
            String json = objectMapper.writeValueAsString(body);
            UserSettings s = settingsService.update(uid, json);

            UserSettingsDto dto = new UserSettingsDto();
            dto.setUserId(s.getUserId());
            dto.setData(body);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
