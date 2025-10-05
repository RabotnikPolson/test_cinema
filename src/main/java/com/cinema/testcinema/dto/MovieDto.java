package com.cinema.testcinema.dto;

public class MovieDto {
    private String title;  // Игнорируется, если есть imdbId (берётся из OMDB)
    private Long year;     // Теперь Long для consistency
    private String imdbId; // Обязательно для автозаполнения
    private Long genreId;

    public MovieDto() {}

    public MovieDto(String title, Long year, String imdbId, Long genreId) {
        this.title = title;
        this.year = year;
        this.imdbId = imdbId;
        this.genreId = genreId;
    }

    // Геттеры и сеттеры
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getYear() { return year; }
    public void setYear(Long year) { this.year = year; }

    public String getImdbId() { return imdbId; }
    public void setImdbId(String imdbId) { this.imdbId = imdbId; }

    public Long getGenreId() { return genreId; }
    public void setGenreId(Long genreId) { this.genreId = genreId; }
}