package com.cinema.testcinema.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class OpenSubtitlesQuotaExceededException extends RuntimeException {
    public OpenSubtitlesQuotaExceededException(String message) {
        super(message);
    }
}
