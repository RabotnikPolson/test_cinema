package com.cinema.testcinema.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ratings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","movie_id"}))
public class Rating {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "movie_id")
    private Movie movie;

    @Column(nullable = false)
    private Short score; // 1..10

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Rating() {}
    public Rating(User user, Movie movie, Short score) {
        this.user = user; this.movie = movie; this.score = score;
    }
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Movie getMovie() { return movie; }
    public void setMovie(Movie movie) { this.movie = movie; }
    public Short getScore() { return score; }
    public void setScore(Short score) { this.score = score; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
