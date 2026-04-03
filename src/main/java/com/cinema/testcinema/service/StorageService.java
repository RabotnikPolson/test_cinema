package com.cinema.testcinema.service;

import java.io.InputStream;

public interface StorageService {
    /**
     * Сохраняет файл в локальное или облачное хранилище.
     * 
     * @param movieId ID фильма (для группировки)
     * @param osFileId уникальный ID файла из OpenSubtitles
     * @param dataStream поток данных (содержимое сырого srt/vtt)
     * @param extension расширение файла (например, ".srt")
     * @return относительный путь к сохраненному файлу
     */
    String storeFile(Long movieId, String osFileId, InputStream dataStream, String extension);

    /**
     * Возвращает поток для чтения сохраненного файла.
     */
    InputStream getFile(String storagePath);

    /**
     * Удаляет файл из хранилища.
     */
    void deleteFile(String storagePath);
}
