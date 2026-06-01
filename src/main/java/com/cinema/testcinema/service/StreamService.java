package com.cinema.testcinema.service;

import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.MovieSubtitle;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.MovieSubtitleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class StreamService {

    private static final Logger log = LoggerFactory.getLogger(StreamService.class);

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

        List<MovieSubtitle> subtitles = movieSubtitleRepository.findByMovieId(movieId);
        boolean hasKazakh = false;
        boolean hasAny = false;

        for (MovieSubtitle sub : subtitles) {
            boolean hasPath = (sub.getS3Path() != null && !sub.getS3Path().isBlank())
                    || (sub.getLocalPath() != null && !sub.getLocalPath().isBlank());
            if (!hasPath) continue;

            if ("kk".equalsIgnoreCase(sub.getLanguage())) {
                hasKazakh = true;
                break;
            }
            hasAny = true;
        }

        if (hasKazakh || hasAny) {
            // Фронтенд использует прокси-эндпоинт, а не этот URL напрямую.
            // Значение нужно только как сигнал о наличии субтитров.
            response.put("subtitleUrl", "proxy");
        }

        return response;
    }

    public byte[] getSubtitleBytes(Long movieId) {
        List<MovieSubtitle> subtitles = movieSubtitleRepository.findByMovieId(movieId);

        MovieSubtitle best = null;
        for (MovieSubtitle sub : subtitles) {
            boolean hasPath = (sub.getS3Path() != null && !sub.getS3Path().isBlank())
                    || (sub.getLocalPath() != null && !sub.getLocalPath().isBlank());
            if (!hasPath) continue;

            if ("kk".equalsIgnoreCase(sub.getLanguage())) {
                best = sub;
                break;
            }
            if (best == null) best = sub;
        }

        if (best == null) return null;

        byte[] raw = null;
        String sourcePath = null;

        if (best.getS3Path() != null && !best.getS3Path().isBlank()) {
            try (InputStream is = s3StorageService.getObjectStream(defaultBucket, best.getS3Path())) {
                raw = is.readAllBytes();
                sourcePath = best.getS3Path();
            } catch (Exception e) {
                log.warn("[Stream] MinIO failed for s3_path={}, falling back to local: {}", best.getS3Path(), e.getMessage());
            }
        }

        if (raw == null && best.getLocalPath() != null && !best.getLocalPath().isBlank()) {
            try {
                Path localFile = Path.of(best.getLocalPath()).toAbsolutePath().normalize();
                raw = Files.readAllBytes(localFile);
                sourcePath = best.getLocalPath();
            } catch (Exception e) {
                log.error("[Stream] Local subtitle file not found for movie {}: {}", movieId, e.getMessage());
            }
        }

        if (raw == null) return null;
        return ensureVtt(raw, sourcePath);
    }

    private static final Pattern SRT_TIMECODE = Pattern.compile("(\\d{2}:\\d{2}:\\d{2}),(\\d{3})");

    private byte[] ensureVtt(byte[] raw, String sourcePath) {
        String content = new String(raw, StandardCharsets.UTF_8)
                .replace("\r\n", "\n")
                .replace("\r", "\n");

        if (content.startsWith("WEBVTT")) {
            return raw;
        }

        String vtt = "WEBVTT\n\n" + SRT_TIMECODE.matcher(content).replaceAll("$1.$2");
        log.info("[Stream] Converted SRT→VTT for path={}", sourcePath);
        return vtt.getBytes(StandardCharsets.UTF_8);
    }
}
