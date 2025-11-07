// src/main/java/com/cinema/testcinema/dto/SubscriptionDto.java
package com.cinema.testcinema.dto;

import java.time.Instant;
import java.util.Map;

public class SubscriptionDto {
    public Long userId;
    public String plan;     // free/pro
    public String status;   // none/active/canceled
    public Instant currentPeriodEnd;
    public Map<String,Object> meta;
    public Instant updatedAt;
}
