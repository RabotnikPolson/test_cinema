package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> { }
