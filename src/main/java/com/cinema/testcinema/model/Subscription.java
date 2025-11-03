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

    private String plan;
    private String status;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String meta;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    // getters/setters
}
