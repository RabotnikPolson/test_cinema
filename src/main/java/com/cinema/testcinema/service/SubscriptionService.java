// src/main/java/com/cinema/testcinema/service/SubscriptionService.java
package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.SubscriptionDto;
import com.cinema.testcinema.model.Subscription;
import com.cinema.testcinema.repository.SubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class SubscriptionService {
    private final SubscriptionRepository repo;
    private final ObjectMapper mapper;

    public SubscriptionService(SubscriptionRepository repo, ObjectMapper mapper) {
        this.repo = repo; this.mapper = mapper;
    }

    public SubscriptionDto getOrDefault(Long userId) {
        var s = repo.findById(userId).orElse(null);
        if (s == null) {
            var d = new SubscriptionDto();
            d.userId = userId; d.plan = "free"; d.status = "none";
            return d;
        }
        return toDto(s);
    }

    public SubscriptionDto subscribePro(Long userId) {
        var s = repo.findById(userId).orElseGet(() -> {
            var x = new Subscription();
            x.setUserId(userId);
            return x;
        });
        s.setPlan("pro");
        s.setStatus("active");
        s.setCurrentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
        s.setUpdatedAt(Instant.now());
        return toDto(repo.save(s));
    }

    public SubscriptionDto cancel(Long userId) {
        var s = repo.findById(userId).orElseThrow();
        s.setStatus("canceled");
        s.setUpdatedAt(Instant.now());
        return toDto(repo.save(s));
    }

    private SubscriptionDto toDto(Subscription s) {
        var dto = new SubscriptionDto();
        dto.userId = s.getUserId();
        dto.plan = s.getPlan();
        dto.status = s.getStatus();
        dto.currentPeriodEnd = s.getCurrentPeriodEnd();
        dto.updatedAt = s.getUpdatedAt();
        try {
            dto.meta = s.getMeta() == null ? Map.of() : mapper.readValue(s.getMeta(), Map.class);
        } catch (Exception e) { dto.meta = Map.of(); }
        return dto;
    }
}
