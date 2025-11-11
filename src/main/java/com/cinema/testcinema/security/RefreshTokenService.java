package com.cinema.testcinema.security;

import com.cinema.testcinema.model.RefreshToken;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
// Refresh tokens are persisted to the database to support rotation and logout. Switch to stateless tokens by
// removing the repository usage and relying solely on JWT validation if requirements change.
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public RefreshToken create(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(jwtService.generateRefreshToken(user));
        refreshToken.setExpiresAt(Instant.now().plus(jwtService.getRefreshTokenTtl()));
        refreshToken.setRevoked(false);
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken validate(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token"));

        if (refreshToken.isRevoked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }

        if (!jwtService.isRefreshToken(token)) {
            log.debug("Provided token is not marked as refresh token");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now()) || jwtService.isTokenExpired(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }

        Long tokenUserId = jwtService.extractUserId(token);
        if (tokenUserId == null || !tokenUserId.equals(refreshToken.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }

        return refreshToken;
    }

    @Transactional
    public RefreshToken rotate(RefreshToken existing) {
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);
        return create(existing.getUser());
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.findByUserIdAndRevokedFalse(userId).forEach(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void purgeExpired() {
        // Позволяет в будущем добавить планировщик для очистки, пока вызывается вручную при логине/рефреше
        refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
