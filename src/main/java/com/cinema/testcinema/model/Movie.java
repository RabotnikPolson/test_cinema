package com.cinema.testcinema.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "movies")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private Long year;

    @Column(unique = true)
    private String imdbId;

    @Column(length = 2000)
    private String description;

    private String posterUrl;
    private String director;

    @Column(length = 1000)
    private String actors;

    // строковый кэш жанров из внешнего API (опционально)
    private String genreText;

    private String language;
    private String country;
    private String imdbRating;
    private String runtime;
    private String released;

    private String rottenTomatoesRating;
    private String metacriticRating;
    private String imdbVotes;

    // Новая связь many-to-many
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "movie_genres",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    public Movie() {}
    public Movie(String title, String imdbId, Long year) {
        this.title = title;
        this.imdbId = imdbId;
        this.year = year;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getYear() { return year; }
    public void setYear(Long year) { this.year = year; }

    public String getImdbId() { return imdbId; }
    public void setImdbId(String imdbId) { this.imdbId = imdbId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }

    public String getActors() { return actors; }
    public void setActors(String actors) { this.actors = actors; }

    public String getGenreText() { return genreText; }
    public void setGenreText(String genreText) { this.genreText = genreText; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getImdbRating() { return imdbRating; }
    public void setImdbRating(String imdbRating) { this.imdbRating = imdbRating; }

    public String getRuntime() { return runtime; }
    public void setRuntime(String runtime) { this.runtime = runtime; }

    public String getReleased() { return released; }
    public void setReleased(String released) { this.released = released; }

    public String getRottenTomatoesRating() { return rottenTomatoesRating; }
    public void setRottenTomatoesRating(String rottenTomatoesRating) { this.rottenTomatoesRating = rottenTomatoesRating; }

    public String getMetacriticRating() { return metacriticRating; }
    public void setMetacriticRating(String metacriticRating) { this.metacriticRating = metacriticRating; }

    public String getImdbVotes() { return imdbVotes; }
    public void setImdbVotes(String imdbVotes) { this.imdbVotes = imdbVotes; }

    public Set<Genre> getGenres() { return genres; }
    public void setGenres(Set<Genre> genres) { this.genres = genres; }
}
