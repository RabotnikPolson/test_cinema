// src/main/java/com/cinema/testcinema/service/UserSettingsService.java
package com.cinema.testcinema.service;

import com.cinema.testcinema.model.UserSettings;
import com.cinema.testcinema.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UserSettingsService {
    private final UserSettingsRepository repo;

    public UserSettingsService(UserSettingsRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public UserSettings getOrCreate(Long userId) {
        return repo.findById(userId).orElseGet(() -> {
            UserSettings s = new UserSettings();
            s.setUserId(userId);
            s.setData("{}");
            s.setUpdatedAt(Instant.now());
            return repo.save(s);
        });
    }

    @Transactional
    public UserSettings update(Long userId, String jsonData) {
        UserSettings s = getOrCreate(userId);
        s.setData(jsonData);
        s.setUpdatedAt(Instant.now());
        return repo.save(s);
    }
}
