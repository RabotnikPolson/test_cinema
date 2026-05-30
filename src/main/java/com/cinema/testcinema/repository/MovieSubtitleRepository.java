package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.MovieSubtitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieSubtitleRepository extends JpaRepository<MovieSubtitle, Long> {

    Optional<MovieSubtitle> findFirstByIsDownloadedFalseOrderByCreatedAtAsc();

    boolean existsByMovieIdAndLanguage(Long movieId, String language);

    List<MovieSubtitle> findByMovieId(Long movieId);

    long countByTranslationStatus(String translationStatus);

    @Query(value = """
            SELECT
                SUM(CASE WHEN translation_status = 'pending'     THEN 1 ELSE 0 END),
                SUM(CASE WHEN translation_status = 'in_progress' THEN 1 ELSE 0 END),
                SUM(CASE WHEN translation_status = 'success'     THEN 1 ELSE 0 END),
                SUM(CASE WHEN translation_status = 'failed'      THEN 1 ELSE 0 END),
                COUNT(*)
            FROM movie_subtitles
            WHERE is_downloaded = true
            """, nativeQuery = true)
    List<Object[]> getSubtitleQueueStats();

    @Query(value = """
            SELECT ms.id,
                   m.title,
                   ms.language,
                   ms.translation_status,
                   ms.created_at::text,
                   ms.lines_translated
            FROM movie_subtitles ms
            JOIN movies m ON m.id = ms.movie_id
            WHERE ms.is_downloaded = true
            ORDER BY
                CASE ms.translation_status
                    WHEN 'in_progress' THEN 1
                    WHEN 'pending'     THEN 2
                    WHEN 'none'        THEN 3
                    WHEN 'failed'      THEN 4
                    WHEN 'success'     THEN 5
                    ELSE 6
                END,
                ms.created_at DESC
            LIMIT 100
            """, nativeQuery = true)
    List<Object[]> getSubtitleQueueRows();
}
