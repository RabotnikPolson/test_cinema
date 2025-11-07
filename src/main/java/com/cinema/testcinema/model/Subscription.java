// src/main/java/com/cinema/testcinema/model/Subscription.java
package com.cinema.testcinema.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "plan")
    private String plan; // free / pro / premium

    @Column(name = "status")
    private String status; // active / canceled / none

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta", columnDefinition = "jsonb")
    private String meta;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    // ====== Getters & Setters ======
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(Instant currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }

    public String getMeta() { return meta; }
    public void setMeta(String meta) { this.meta = meta; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
