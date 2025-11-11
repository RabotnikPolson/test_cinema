package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);
    void deleteByUserId(Long userId);
    void deleteByExpiresAtBefore(Instant time);
}
