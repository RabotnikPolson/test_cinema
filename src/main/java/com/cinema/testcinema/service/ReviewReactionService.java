package com.cinema.testcinema.service;

import com.cinema.testcinema.model.Review;
import com.cinema.testcinema.model.ReviewReaction;
import com.cinema.testcinema.model.ReviewReactionType;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.repository.ReviewReactionRepository;
import com.cinema.testcinema.repository.ReviewRepository;
import com.cinema.testcinema.security.AuthenticatedUserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReviewReactionService {

    private final ReviewRepository reviewRepository;
    private final ReviewReactionRepository reviewReactionRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public record ReviewReactionSummaryResponse(Long reviewId, long upVotes, long downVotes) {
    }

    public ReviewReactionService(ReviewRepository reviewRepository,
                                 ReviewReactionRepository reviewReactionRepository,
                                 AuthenticatedUserService authenticatedUserService) {
        this.reviewRepository = reviewRepository;
        this.reviewReactionRepository = reviewReactionRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    public ReviewReactionSummaryResponse toggleReaction(Long reviewId, ReviewReactionType type, Authentication auth) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Отзыв не найден"));

        Long userId = authenticatedUserService.requireCurrentUserId(auth);
        User userRef = new User();
        userRef.setId(userId);

        ReviewReaction existing = reviewReactionRepository.findByReviewIdAndUserId(reviewId, userId)
                .orElse(null);

        if (existing == null) {
            ReviewReaction reaction = new ReviewReaction();
            reaction.setReview(review);
            reaction.setUser(userRef);
            reaction.setType(type);
            reviewReactionRepository.save(reaction);
        } else if (existing.getType() == type) {
            reviewReactionRepository.delete(existing);
        } else {
            existing.setType(type);
            reviewReactionRepository.save(existing);
        }

        long upVotes = reviewReactionRepository.countByReviewIdAndType(reviewId, ReviewReactionType.UP);
        long downVotes = reviewReactionRepository.countByReviewIdAndType(reviewId, ReviewReactionType.DOWN);
        return new ReviewReactionSummaryResponse(reviewId, upVotes, downVotes);
    }
}
