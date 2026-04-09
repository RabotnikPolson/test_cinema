package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.MovieSubtitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MovieSubtitleRepository extends JpaRepository<MovieSubtitle, Long> {
    
    Optional<MovieSubtitle> findFirstByIsDownloadedFalseOrderByCreatedAtAsc();

    boolean existsByMovieIdAndLanguage(Long movieId, String language);
}
