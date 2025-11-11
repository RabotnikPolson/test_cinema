package com.cinema.testcinema.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "review_id")
    private Review review; // null => коммент к фильму

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id")
    private Comment parent;

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
    public Review getReview(){ return review; }
    public void setReview(Review review){ this.review = review; }
    public Comment getParent(){ return parent; }
    public void setParent(Comment parent){ this.parent = parent; }
    public User getUser(){ return user; }
    public void setUser(User user){ this.user = user; }
    public String getBody(){ return body; }
    public void setBody(String body){ this.body = body; }
    public Instant getCreatedAt(){ return createdAt; }
    public Instant getUpdatedAt(){ return updatedAt; }
}
