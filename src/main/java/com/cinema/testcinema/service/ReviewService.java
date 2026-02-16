package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.review.ReviewCreateRequest;
import com.cinema.testcinema.dto.review.ReviewResponse;
import com.cinema.testcinema.dto.review.ReviewUpdateRequest;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.Review;
import com.cinema.testcinema.model.ReviewReactionType;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.RatingRepository;
import com.cinema.testcinema.repository.ReviewReactionRepository;
import com.cinema.testcinema.repository.ReviewRepository;
import com.cinema.testcinema.repository.UserProfileRepository;
import com.cinema.testcinema.repository.UserRepository;
import com.cinema.testcinema.security.AuthenticatedUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;
    private final RatingRepository ratingRepository;
    private final ReviewReactionRepository reviewReactionRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public ReviewService(ReviewRepository reviewRepository,
                         MovieRepository movieRepository,
                         RatingRepository ratingRepository,
                         ReviewReactionRepository reviewReactionRepository,
                         UserRepository userRepository,
                         UserProfileRepository userProfileRepository,
                         AuthenticatedUserService authenticatedUserService) {
        this.reviewRepository = reviewRepository;
        this.movieRepository = movieRepository;
        this.ratingRepository = ratingRepository;
        this.reviewReactionRepository = reviewReactionRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Transactional
    public ReviewResponse create(ReviewCreateRequest request, Authentication authentication) {
        Long currentUserId = authenticatedUserService.requireCurrentUserId(authentication);
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Пользователь с ID " + currentUserId + " не найден"));

        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Фильм с ID " + request.movieId() + " не найден"));

        if (request.parentId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Replies to reviews are no longer supported");
        }

        Review review = new Review();
        review.setMovie(movie);
        review.setUser(user);
        review.setContent(request.content().trim());

        Review saved = reviewRepository.save(review);
        return toResponse(saved, List.of());
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listByMovie(Long movieId, Pageable pageable) {
        ensureMovieExists(movieId);
        Page<Review> roots = reviewRepository.findByMovieIdAndParentIdIsNull(movieId, pageable);
        List<Long> rootIds = roots.stream().map(Review::getId).filter(Objects::nonNull).toList();
        Map<Long, List<ReviewResponse>> repliesMap = loadReplies(rootIds);
        return roots.map(review -> toResponse(review, repliesMap.getOrDefault(review.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public ReviewResponse get(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Отзыв с ID " + id + " не найден"));
        Map<Long, List<ReviewResponse>> replies = loadReplies(List.of(review.getId()));
        return toResponse(review, replies.getOrDefault(review.getId(), List.of()));
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReplies(Long id, Pageable pageable) {
        Review parent = reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Отзыв с ID " + id + " не найден"));
        Page<Review> page = reviewRepository.findByParentId(parent.getId(), pageable);
        return page.map(reply -> toResponse(reply, List.of()));
    }

    @Transactional
    public ReviewResponse update(Long id, ReviewUpdateRequest request, Authentication authentication) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Отзыв с ID " + id + " не найден"));

        Long currentUserId = authenticatedUserService.requireCurrentUserId(authentication);
        boolean isAdmin = authenticatedUserService.hasRole(authentication, "ADMIN");
        if (!isAdmin && !Objects.equals(getUserIdSafe(review), currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Instant now = Instant.now();
        if (Duration.between(review.getCreatedAt(), now).compareTo(Duration.ofHours(1)) > 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Отзыв можно редактировать только в течение часа после создания");
        }

        review.setContent(request.content().trim());
        review.setEdited(true);
        review.setUpdatedAt(now);

        Review saved = reviewRepository.save(review);
        Map<Long, List<ReviewResponse>> replies = loadReplies(List.of(saved.getId()));
        return toResponse(saved, replies.getOrDefault(saved.getId(), List.of()));
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Отзыв с ID " + id + " не найден"));

        Long currentUserId = authenticatedUserService.requireCurrentUserId(authentication);
        boolean isAdmin = authenticatedUserService.hasRole(authentication, "ADMIN");
        if (!isAdmin && !Objects.equals(getUserIdSafe(review), currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        reviewRepository.delete(review);
    }

    private Map<Long, List<ReviewResponse>> loadReplies(List<Long> parentIds) {
        if (parentIds.isEmpty()) return Map.of();
        List<Review> replies = reviewRepository.findByParentIdIn(parentIds);
        replies.sort(Comparator.comparing(Review::getCreatedAt));
        return replies.stream()
                .map(reply -> toResponse(reply, List.of()))
                .collect(Collectors.groupingBy(ReviewResponse::getParentId, Collectors.toCollection(ArrayList::new)));
    }

    private void ensureMovieExists(Long movieId) {
        if (!movieRepository.existsById(movieId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Фильм с ID " + movieId + " не найден");
        }
    }

    private Long getUserIdSafe(Review review) {
        if (review.getUser() != null && review.getUser().getId() != null) return review.getUser().getId();
        return review.getUserId();
    }

    private Long getMovieIdSafe(Review review) {
        if (review.getMovie() != null && review.getMovie().getId() != null) return review.getMovie().getId();
        return review.getMovieId();
    }

    private ReviewResponse toResponse(Review review, List<ReviewResponse> replies) {
        Long userId = getUserIdSafe(review);
        Long movieId = getMovieIdSafe(review);

        Short score = (userId != null && movieId != null)
                ? ratingRepository.findByUserIdAndMovieId(userId, movieId)
                .map(r -> r.getScore()).orElse(null)
                : null;

        long upVotes = (review.getId() != null)
                ? reviewReactionRepository.countByReviewIdAndType(review.getId(), ReviewReactionType.UP)
                : 0;

        long downVotes = (review.getId() != null)
                ? reviewReactionRepository.countByReviewIdAndType(review.getId(), ReviewReactionType.DOWN)
                : 0;

        ReviewResponse response = new ReviewResponse(
                review.getId(),
                userId,
                movieId,
                review.getParentId(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                review.isEdited(),
                score,
                upVotes,
                downVotes
        );

        response.setReplies(replies);

        if (review.getUser() != null) {
            response.setAuthorUsername(review.getUser().getUsername());
            if (userId != null) {
                userProfileRepository.findByUserId(userId)
                        .ifPresent(profile -> response.setAuthorAvatarUrl(profile.getAvatarUrl()));
            }
        }

        return response;
    }
}
