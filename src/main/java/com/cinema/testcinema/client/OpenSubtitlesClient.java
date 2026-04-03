package com.cinema.testcinema.client;

import com.cinema.testcinema.dto.subtitle.DownloadLinkDto;
import com.cinema.testcinema.dto.subtitle.SubtitleInfoDto;

import java.util.List;

public interface OpenSubtitlesClient {
    /**
     * Авторизация. Возвращает Bearer token.
     */
    String login();

    /**
     * Поиск субтитров без расхода квоты скачиваний.
     * @param imdbId ID фильма (например, "0111161")
     * @param originalLanguages список языков (например, "en", "ru")
     * @return список найденных субтитров
     */
    List<SubtitleInfoDto> searchSubtitles(String imdbId, List<String> originalLanguages);

    /**
     * Получить прямую ссылку на скачивание. Расходует квоту!
     * @param osFileId уникальный ID файла
     * @return DTO со ссылкой на загрузку
     */
    DownloadLinkDto requestDownloadLink(String osFileId);
}
