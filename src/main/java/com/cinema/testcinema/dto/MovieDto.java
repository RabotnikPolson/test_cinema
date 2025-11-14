package com.cinema.testcinema.dto;

public class MovieDto {
    private String title;
    private int year;
    private String imdbId;
    private Long genreId;

    // публичный URL для плеера: "/hls/Screen/index.m3u8"
    private String streamUrl;

    public MovieDto() {}

    public MovieDto(String title, int year, String imdbId, Long genreId) {
        this.title = title;
        this.year = year;
        this.imdbId = imdbId;
        this.genreId = genreId;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getImdbId() { return imdbId; }
    public void setImdbId(String imdbId) { this.imdbId = imdbId; }

    public Long getGenreId() { return genreId; }
    public void setGenreId(Long genreId) { this.genreId = genreId; }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }
}
