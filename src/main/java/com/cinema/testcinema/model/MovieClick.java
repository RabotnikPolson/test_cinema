package com.cinema.testcinema.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "movie_clicks")
public class MovieClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", updatable = false)
    private Long userId;

    @Column(name = "guest_session_id", updatable = false)
    private String guestSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false, updatable = false)
    private Movie movie;

    @Column(name = "clicked_at", nullable = false, updatable = false)
    private Instant clickedAt = Instant.now();

    public MovieClick() {}

    public MovieClick(Long userId, String guestSessionId, Movie movie) {
        this.userId = userId;
        this.guestSessionId = guestSessionId;
        this.movie = movie;
        this.clickedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getGuestSessionId() { return guestSessionId; }
    public Movie getMovie() { return movie; }
    public Instant getClickedAt() { return clickedAt; }
}
