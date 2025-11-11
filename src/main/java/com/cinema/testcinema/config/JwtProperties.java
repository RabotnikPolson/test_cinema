package com.cinema.testcinema.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    @NotBlank
    private String secret;

    @Min(1)
    private long accessTtlMin = 30;

    @Min(1)
    private long refreshTtlDays = 30;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTtlMin() {
        return accessTtlMin;
    }

    public void setAccessTtlMin(long accessTtlMin) {
        this.accessTtlMin = accessTtlMin;
    }

    public long getRefreshTtlDays() {
        return refreshTtlDays;
    }

    public void setRefreshTtlDays(long refreshTtlDays) {
        this.refreshTtlDays = refreshTtlDays;
    }

    public Duration accessTokenTtl() {
        return Duration.ofMinutes(accessTtlMin);
    }

    public Duration refreshTokenTtl() {
        return Duration.ofDays(refreshTtlDays);
    }
}
