package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByMovieIdAndParentIdIsNull(Long movieId, Pageable pageable);

    List<Review> findByParentIdIn(Collection<Long> parentIds);

    Page<Review> findByParentId(Long parentId, Pageable pageable);

    long countByParentId(Long parentId);
}
