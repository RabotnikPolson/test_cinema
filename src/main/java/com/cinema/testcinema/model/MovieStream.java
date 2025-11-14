package com.cinema.testcinema.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "movie_streams")
public class MovieStream {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "imdb_id", nullable = false, unique = true, length = 32)
    private String imdbId;

    // относительный путь внутри /hls, например: "Screen/index.m3u8"
    @Column(name = "stream_path", nullable = false)
    private String streamPath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public MovieStream() {
    }

    public MovieStream(String imdbId, String streamPath) {
        this.imdbId = imdbId;
        this.streamPath = streamPath;
    }

    public Long getId() {
        return id;
    }

    public String getImdbId() {
        return imdbId;
    }

    public void setImdbId(String imdbId) {
        this.imdbId = imdbId;
    }

    public String getStreamPath() {
        return streamPath;
    }

    public void setStreamPath(String streamPath) {
        this.streamPath = streamPath;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
