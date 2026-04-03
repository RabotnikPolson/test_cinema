package com.cinema.testcinema.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class LocalStorageService implements StorageService {

    @Value("${storage.local.base-path:./storage}")
    private String basePath;

    @Override
    public String storeFile(Long movieId, String osFileId, InputStream dataStream, String extension) {
        String relativeDir = "movies/" + movieId + "/subtitles";
        String fileName = osFileId + extension;
        String relativePath = relativeDir + "/" + fileName;

        Path absoluteDirPath = Paths.get(basePath, relativeDir);
        Path absoluteFilePath = absoluteDirPath.resolve(fileName);

        try {
            Files.createDirectories(absoluteDirPath);
            Files.copy(dataStream, absoluteFilePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved to local storage: {}", absoluteFilePath);
            return relativePath;
        } catch (IOException e) {
            log.error("Failed to store file: {}", relativePath, e);
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public InputStream getFile(String storagePath) {
        Path absolutePath = Paths.get(basePath, storagePath);
        try {
            return new FileInputStream(absolutePath.toFile());
        } catch (FileNotFoundException e) {
            log.error("File not found: {}", absolutePath);
            throw new RuntimeException("File not found", e);
        }
    }

    @Override
    public void deleteFile(String storagePath) {
        Path absolutePath = Paths.get(basePath, storagePath);
        try {
            boolean deleted = Files.deleteIfExists(absolutePath);
            if (deleted) {
                log.info("Deleted file: {}", absolutePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", absolutePath, e);
        }
    }
}
