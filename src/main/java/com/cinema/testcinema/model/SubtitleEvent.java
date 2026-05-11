package com.cinema.testcinema.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "subtitle_events")
public class SubtitleEvent {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private SubtitleAction action;

    @Column(length = 10, updatable = false)
    private String lang;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public SubtitleEvent() {}

    public SubtitleEvent(Long userId, String guestSessionId, Movie movie, SubtitleAction action, String lang) {
        this.userId = userId;
        this.guestSessionId = guestSessionId;
        this.movie = movie;
        this.action = action;
        this.lang = lang;
        this.createdAt = Instant.now();
    }

    public enum SubtitleAction {
        enable, disable, change_lang
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getGuestSessionId() { return guestSessionId; }
    public Movie getMovie() { return movie; }
    public SubtitleAction getAction() { return action; }
    public String getLang() { return lang; }
    public Instant getCreatedAt() { return createdAt; }
}
