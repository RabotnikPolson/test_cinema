package com.cinema.testcinema.dto;

public class MovieDto {
    private String title;
    private int year;        // int, а не long
    private String imdbId;
    private Long genreId;

    public MovieDto() {}

    public MovieDto(String title, int year, String imdbId, Long genreId) {
        this.title = title;
        this.year = year;
        this.imdbId = imdbId;
        this.genreId = genreId;
    }

    // Геттеры и сеттеры
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getYear() { return year; }      // исправлено
    public void setYear(int year) { this.year = year; }

    public String getImdbId() { return imdbId; }
    public void setImdbId(String imdbId) { this.imdbId = imdbId; }

    public Long getGenreId() { return genreId; }
    public void setGenreId(Long genreId) { this.genreId = genreId; }
}

