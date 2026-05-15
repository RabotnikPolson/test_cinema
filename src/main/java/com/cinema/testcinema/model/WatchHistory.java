package com.cinema.testcinema.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "watch_history", indexes = {
        @Index(name="idx_wh_user_time", columnList = "user_id, started_at"),
        @Index(name="idx_wh_movie_time", columnList = "movie_id, started_at"),
        @Index(name="idx_wh_session", columnList = "session_id")
})
public class WatchHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id")
    private User user;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="movie_id", nullable=false)
    private Movie movie;

    @Column(name="session_id", nullable=false)
    private UUID sessionId;

    @Column(name="started_at", nullable=false)
    private Instant startedAt;

    @Column(name="seconds_watched", nullable=false)
    private int secondsWatched;

    @Column(name="completed", nullable=false)
    private boolean completed = false;

    @Column(name="last_beat_at", nullable=false)
    private Instant lastBeatAt = Instant.now();

    public Long getId(){return id;}
    public User getUser(){return user;}
    public void setUser(User user){this.user=user;}
    public Movie getMovie(){return movie;}
    public void setMovie(Movie movie){this.movie=movie;}
    public UUID getSessionId(){return sessionId;}
    public void setSessionId(UUID sessionId){this.sessionId=sessionId;}
    public Instant getStartedAt(){return startedAt;}
    public void setStartedAt(Instant startedAt){this.startedAt=startedAt;}
    public int getSecondsWatched(){return secondsWatched;}
    public void setSecondsWatched(int secondsWatched){this.secondsWatched=secondsWatched;}
    public boolean isCompleted(){return completed;}
    public void setCompleted(boolean completed){this.completed=completed;}
    public Instant getLastBeatAt(){return lastBeatAt;}
    public void setLastBeatAt(Instant lastBeatAt){this.lastBeatAt=lastBeatAt;}
}
