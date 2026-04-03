package com.cinema.testcinema.service;

public interface SubtitleSyncService {
    /**
     * Вызывается при добавлении фильма. Ищет EN/RU сабы и делает запись в БД 
     * с флагами is_downloaded=false, needs_translation=true.
     * 
     * @param movieId внутренний ID фильма
     * @param imdbId внешний ID (imdb_id или kinopoisk аналог) для поиска в OpenSubtitles
     */
    void discoverAndStageSubtitles(Long movieId, String imdbId);

    /**
     * Вызывается фоновым воркером для скачивания конкретного файла из очереди.
     * 
     * @param subtitleId ID сущности MovieSubtitle
     */
    void processStagedSubtitleDownload(Long subtitleId);
}
