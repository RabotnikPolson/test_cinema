package com.cinema.testcinema.dto;

public class MovieDto {
    private String title;
    private Long year;
    private String imdbId;
    private Long genreId;
    private String type; // "movie" или "series"

    public MovieDto() {}

    public MovieDto(String title, Long year, String imdbId, Long genreId, String type) {
        this.title = title;
        this.year = year;
        this.imdbId = imdbId;
        this.genreId = genreId;
        this.type = type;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Long getYear() { return year; }
    public void setYear(Long year) { this.year = year; }

    public String getImdbId() { return imdbId; }
    public void setImdbId(String imdbId) { this.imdbId = imdbId; }

    public Long getGenreId() { return genreId; }
    public void setGenreId(Long genreId) { this.genreId = genreId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
