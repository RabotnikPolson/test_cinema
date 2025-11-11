package com.cinema.testcinema.security;

import com.cinema.testcinema.config.JwtProperties;
import com.cinema.testcinema.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtProperties properties;
    private final Key signingKey;

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

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String email = extractEmail(token);
        return email.equalsIgnoreCase(userDetails.getUsername()) && !isTokenExpired(token);
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
        Jws<Claims> claimsJws = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);
        return claimsJws.getBody();
    }

    private Key buildKey(String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            log.debug("JWT secret is not Base64 encoded, falling back to raw bytes");
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256 bits");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
