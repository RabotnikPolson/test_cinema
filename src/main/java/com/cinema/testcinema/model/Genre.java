package com.cinema.testcinema.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "genres")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "genre", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference
    private List<Movie> movies = new ArrayList<>();

    // Конструкторы
    public Genre() {}

    public Genre(String name) {
        this.name = name;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Movie> getMovies() { return movies; }
    public void setMovies(List<Movie> movies) { this.movies = movies; }

    // Методы для управления фильмами
    public void addMovie(Movie movie) {
        movies.add(movie);
        movie.setGenre(this);
    }

    public void removeMovie(Movie movie) {
        movies.remove(movie);
        movie.setGenre(null);
    }
}