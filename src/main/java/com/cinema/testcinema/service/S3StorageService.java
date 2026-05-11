package com.cinema.testcinema.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Сервис для интеграции с S3-совместимым хранилищем MinIO.
 */
@Service
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final MinioClient minioClient;

    public S3StorageService(
            @Value("${minio.url}") String url,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {
        this.minioClient = MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * Генерация временной ссылки для доступа к приватному файлу в бакете.
     * Используется для стриминга видео и загрузки субтитров на клиент.
     *
     * @param bucketName Имя бакета
     * @param objectPath Путь к файлу в бакете (например, movies/123/video.mp4)
     * @param expiryHours Время жизни ссылки в часах
     * @return Presigned URL в виде строки
     */
    public String generatePresignedUrl(String bucketName, String objectPath, int expiryHours) {
        if (objectPath == null || objectPath.isBlank()) {
            return null;
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectPath)
                            .expiry(expiryHours, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.error("[S3] Ошибка генерации ссылки для s3://{}/{}: {}", bucketName, objectPath, e.getMessage());
            return null;
        }
    }
}
