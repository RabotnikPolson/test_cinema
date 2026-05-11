package com.cinema.testcinema.service;

import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.MovieSubtitle;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.MovieSubtitleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StreamService {

    private final MovieRepository movieRepository;
    private final MovieSubtitleRepository movieSubtitleRepository;
    private final S3StorageService s3StorageService;

    @Value("${minio.bucket.default}")
    private String defaultBucket;

    public StreamService(MovieRepository movieRepository,
                         MovieSubtitleRepository movieSubtitleRepository,
                         S3StorageService s3StorageService) {
        this.movieRepository = movieRepository;
        this.movieSubtitleRepository = movieSubtitleRepository;
        this.s3StorageService = s3StorageService;
    }

    public Map<String, String> getStreamUrls(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм с ID " + movieId + " не найден"));

        Map<String, String> response = new HashMap<>();
        response.put("type", "minio");

        // 1. Ссылка на видео
        String videoS3Path = movie.getVideoS3Path();
        if (videoS3Path != null && !videoS3Path.isBlank()) {
            String videoUrl = s3StorageService.generatePresignedUrl(defaultBucket, videoS3Path, 3);
            if (videoUrl != null) {
                response.put("videoUrl", videoUrl);
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка генерации ссылки на видео");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Видео для этого фильма еще не загружено (video_s3_path пуст)");
        }

        // 2. Ссылка на субтитры
        List<MovieSubtitle> subtitles = movieSubtitleRepository.findByMovieId(movieId);
        String subtitleUrl = null;

        for (MovieSubtitle sub : subtitles) {
            String s3Path = sub.getS3Path();
            if (s3Path != null && !s3Path.isBlank()) {
                if ("kk".equalsIgnoreCase(sub.getLanguage())) {
                    subtitleUrl = s3StorageService.generatePresignedUrl(defaultBucket, s3Path, 3);
                    break; // Нашли казахские субтитры - отдаем наивысший приоритет
                } else if (subtitleUrl == null) {
                    // Запоминаем первые попавшиеся субтитры на случай, если нет казахских
                    subtitleUrl = s3StorageService.generatePresignedUrl(defaultBucket, s3Path, 3);
                }
            }
        }

        if (subtitleUrl != null) {
            response.put("subtitleUrl", subtitleUrl);
        }

        return response;
    }
}
