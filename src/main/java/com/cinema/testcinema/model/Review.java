package com.cinema.testcinema.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"movie_id","user_id"}))
public class Review {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate void touch(){ this.updatedAt = Instant.now(); }

    // getters/setters
    public Long getId(){ return id; }
    public Movie getMovie(){ return movie; }
    public void setMovie(Movie movie){ this.movie = movie; }
    public User getUser(){ return user; }
    public void setUser(User user){ this.user = user; }
    public String getBody(){ return body; }
    public void setBody(String body){ this.body = body; }
    public Instant getCreatedAt(){ return createdAt; }
    public Instant getUpdatedAt(){ return updatedAt; }
}
