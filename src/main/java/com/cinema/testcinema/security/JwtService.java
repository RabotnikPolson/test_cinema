package com.cinema.testcinema.security;

import com.cinema.testcinema.config.JwtProperties;
import com.cinema.testcinema.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = buildKey(properties.getSecret());
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("roles", user.getRoles().stream()
                .map(role -> role.getRoleName())
                .collect(Collectors.toList()));
        claims.put("type", "access");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(user.getId()))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(getAccessTokenTtl())))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("type", "refresh");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(user.getId()))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(getRefreshTokenTtl())))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

//    public boolean isTokenValid(String token, UserDetails userDetails) {
//        String email = extractEmail(token);
//        return email.equalsIgnoreCase(userDetails.getUsername()) && !isTokenExpired(token);
//    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            var jws = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);
            Date exp = jws.getBody().getExpiration();
            return exp != null && exp.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }


    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    public Long extractUserId(String token) {
        String subject = extractAllClaims(token).getSubject();
        return subject != null ? Long.parseLong(subject) : null;
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(extractAllClaims(token).get("type", String.class));
        } catch (ExpiredJwtException ex) {
            return "refresh".equals(ex.getClaims().get("type", String.class));
        }
    }

    public Duration getAccessTokenTtl() {
        return properties.accessTokenTtl();
    }

    public Duration getRefreshTokenTtl() {
        return properties.refreshTokenTtl();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey buildKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is empty or missing");
        }

        byte[] keyBytes;

        // 1) Base64
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception ignore1) {
            // 2) Base64URL
            try {
                keyBytes = Decoders.BASE64URL.decode(secret);
            } catch (Exception ignore2) {
                // 3) Обычная строка — усилим через SHA-256 чтобы получить 32 байта
                keyBytes = hashTo32Bytes(secret.getBytes(StandardCharsets.UTF_8));
            }
        }

        // Если вдруг ключ < 32 байт — усиливаем
        if (keyBytes.length < 32) {
            keyBytes = hashTo32Bytes(keyBytes);
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] hashTo32Bytes(byte[] input) {
        try {
            return Arrays.copyOf(MessageDigest.getInstance("SHA-256").digest(input), 32);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create JWT signing key", e);
        }
    }
}
