package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByMovieIdAndParentIdIsNull(Long movieId, Pageable pageable);

    List<Comment> findByParentIdIn(Collection<Long> parentIds);

    Page<Comment> findByParentId(Long parentId, Pageable pageable);

    long countByParentId(Long parentId);
}
