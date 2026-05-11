package com.cinema.testcinema.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "movie_subtitles")
public class MovieSubtitle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(name = "os_file_id", nullable = false, unique = true, length = 50)
    private String osFileId;

    @Column(name = "local_path", length = 500)
    private String localPath;

    @Column(name = "is_downloaded", nullable = false)
    private boolean isDownloaded = false;

    @Column(name = "translation_status", length = 20)
    private String translationStatus = "none";

    @Column(name = "translated_path", length = 500)
    private String translatedPath;

    @Column(name = "lines_translated")
    private Integer linesTranslated = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "s3_path", length = 500)
    private String s3Path;

    public MovieSubtitle() {
    }

    public MovieSubtitle(Movie movie, String language, String osFileId) {
        this.movie = movie;
        this.language = language;
        this.osFileId = osFileId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getOsFileId() {
        return osFileId;
    }

    public void setOsFileId(String osFileId) {
        this.osFileId = osFileId;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    public String getTranslationStatus() {
        return translationStatus;
    }

    public void setTranslationStatus(String translationStatus) {
        this.translationStatus = translationStatus;
    }

    public String getTranslatedPath() {
        return translatedPath;
    }

    public void setTranslatedPath(String translatedPath) {
        this.translatedPath = translatedPath;
    }

    public Integer getLinesTranslated() {
        return linesTranslated;
    }

    public void setLinesTranslated(Integer linesTranslated) {
        this.linesTranslated = linesTranslated;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getS3Path() {
        return s3Path;
    }

    public void setS3Path(String s3Path) {
        this.s3Path = s3Path;
    }
}
