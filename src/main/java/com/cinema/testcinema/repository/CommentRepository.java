package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = {"user","parent"})
    Page<Comment> findByMovieImdbIdAndReviewIsNullOrderByCreatedAtDesc(String imdbId, Pageable p);

    @EntityGraph(attributePaths = {"user","parent"})
    Page<Comment> findByReviewIdOrderByCreatedAtDesc(Long reviewId, Pageable p);
}
