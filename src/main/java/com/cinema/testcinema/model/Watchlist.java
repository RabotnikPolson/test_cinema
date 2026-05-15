package com.cinema.testcinema.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "watchlists")
@IdClass(Watchlist.WatchlistId.class)
public class Watchlist {

    @Id
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id")
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "movie_id")
    private Movie movie;

    @Column(nullable = false)
    private Instant addedAt = Instant.now();

    public Watchlist() {}
    public Watchlist(User user, Movie movie) { this.user = user; this.movie = movie; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Movie getMovie() { return movie; }
    public void setMovie(Movie movie) { this.movie = movie; }
    public Instant getAddedAt() { return addedAt; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }

    // составной ключ
    public static class WatchlistId implements Serializable {
        private Long user;  // id User
        private Long movie; // id Movie
        public WatchlistId() {}
        public WatchlistId(Long user, Long movie) { this.user=user; this.movie=movie; }
        @Override public boolean equals(Object o){
            if (this==o) return true;
            if (!(o instanceof WatchlistId that)) return false;
            return Objects.equals(user, that.user) && Objects.equals(movie, that.movie);
        }
        @Override public int hashCode(){ return Objects.hash(user, movie); }
    }
}
