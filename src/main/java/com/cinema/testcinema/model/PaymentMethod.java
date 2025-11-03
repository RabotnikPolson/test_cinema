package com.cinema.testcinema.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "payment_methods")
public class PaymentMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String brand;
    private String last4;
    private Integer expMonth;
    private Integer expYear;
    private Boolean isDefault;
    private String tokenRef;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    // getters/setters
}
