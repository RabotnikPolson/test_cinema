package com.cinema.testcinema.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

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

    private String genreText; // текстовое поле "Action, Comedy, Drama" из API

    private String language;

    private String country;

    private String imdbRating;

    private String runtime;

    private String released;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id")
    @JsonBackReference
    private Genre genre;

    public Movie() {}

    public Movie(String title, String imdbId, Long year) {
        this.title = title;
        this.imdbId = imdbId;
        this.year = year;
    }

    public Movie(String title, String imdbId, Long year, String description, String posterUrl, Genre genre) {
        this.title = title;
        this.imdbId = imdbId;
        this.year = year;
        this.description = description;
        this.posterUrl = posterUrl;
        this.genre = genre;
    }

    // Геттеры и сеттеры
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

    public Genre getGenre() { return genre; }
    public void setGenre(Genre genre) { this.genre = genre; }
}
