package com.cinema.testcinema.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SubtitleWebhookRequest {

    @JsonProperty("movie_id")
    private Long movieId;

    private String language;
    private String status;

    @JsonProperty("output_path")
    private String outputPath;

    @JsonProperty("lines_translated")
    private Integer linesTranslated;

    @JsonProperty("error_message")
    private String errorMessage;

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public Integer getLinesTranslated() {
        return linesTranslated;
    }

    public void setLinesTranslated(Integer linesTranslated) {
        this.linesTranslated = linesTranslated;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
