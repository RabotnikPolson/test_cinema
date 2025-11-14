package com.cinema.testcinema.dto;

public class MovieStreamDto {

    private Long id;
    private String imdbId;
    // относительный путь: "Screen/index.m3u8"
    private String streamPath;

    public MovieStreamDto() {
    }

    public MovieStreamDto(Long id, String imdbId, String streamPath) {
        this.id = id;
        this.imdbId = imdbId;
        this.streamPath = streamPath;
    }

    public Long getId() {
        return id;
    }

    public String getImdbId() {
        return imdbId;
    }

    public String getStreamPath() {
        return streamPath;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    public void setStreamPath(String streamPath) {
        this.streamPath = streamPath;
    }
}
