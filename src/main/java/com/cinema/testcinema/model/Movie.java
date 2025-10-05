package com.cinema.testcinema.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "movies")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column
    private Long year;

    @Column(unique = true)
    private String imdbId;

    @Column(length = 2000)
    private String description;

    private String posterUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id")
    @JsonBackReference
    private Genre genre;

    // Конструкторы
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

    public Genre getGenre() { return genre; }
    public void setGenre(Genre genre) { this.genre = genre; }
}