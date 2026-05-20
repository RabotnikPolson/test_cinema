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
}
