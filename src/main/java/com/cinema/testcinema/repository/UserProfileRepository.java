// src/main/java/com/cinema/testcinema/repository/UserProfileRepository.java
package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> { }
