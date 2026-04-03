package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.MovieSubtitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubtitleRepository extends JpaRepository<MovieSubtitle, Long> {
    Optional<MovieSubtitle> findByOsFileId(String osFileId);

    // Запрос для воркера: найти субтитры, которые нужно скачать
    @Query("SELECT s FROM MovieSubtitle s WHERE s.isDownloaded = false ORDER BY s.movie.kzCulturalWeight DESC, s.createdAt ASC")
    List<MovieSubtitle> findPendingDownloads();
}
