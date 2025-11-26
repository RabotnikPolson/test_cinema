package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    List<EmailVerificationToken> findByUserIdAndConsumedFalse(Long userId);
    Optional<EmailVerificationToken> findTopByUserIdAndConsumedFalseOrderByCreatedAtDesc(Long userId);
    List<EmailVerificationToken> findByUserIdAndExpiresAtBefore(Long userId, Instant expiresAt);
}
