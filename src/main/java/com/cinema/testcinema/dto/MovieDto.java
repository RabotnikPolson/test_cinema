package com.cinema.testcinema.dto;

public class MovieDto {
    private String title;
    private int year; // int, а не long
    private String imdbId;
    private String kinopoiskId;
    private Long genreId;

    public MovieDto() {
    }

    public MovieDto(String title, int year, String imdbId, String kinopoiskId, Long genreId) {
        this.title = title;
        this.year = year;
        this.imdbId = imdbId;
        this.kinopoiskId = kinopoiskId;
        this.genreId = genreId;
    }

    // Геттеры и сеттеры
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getYear() {
        return year;
    } // исправлено

    public void setYear(int year) {
        this.year = year;
    }

    public String getImdbId() {
        return imdbId;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    public String getKinopoiskId() {
        return kinopoiskId;
    }

    public void setKinopoiskId(String kinopoiskId) {
        this.kinopoiskId = kinopoiskId;
    }

    public Long getGenreId() {
        return genreId;
    }

    public void setGenreId(Long genreId) {
        this.genreId = genreId;
    }
}
