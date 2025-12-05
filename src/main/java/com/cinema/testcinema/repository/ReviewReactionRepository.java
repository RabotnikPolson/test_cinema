package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.ReviewReaction;
import com.cinema.testcinema.model.ReviewReactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewReactionRepository extends JpaRepository<ReviewReaction, Long> {
    Optional<ReviewReaction> findByReviewIdAndUserId(Long reviewId, Long userId);

    List<ReviewReaction> findByReviewIdIn(Collection<Long> reviewIds);

    long countByReviewIdAndType(Long reviewId, ReviewReactionType type);
}
