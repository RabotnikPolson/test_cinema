package com.cinema.testcinema.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "analytics_daily")
public class AnalyticsDaily {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id")
    private Long userId;
    private LocalDate d;
    private Integer plays = 0;
    private Integer likes = 0;
    private Integer shares = 0;
    // getters/setters
}
