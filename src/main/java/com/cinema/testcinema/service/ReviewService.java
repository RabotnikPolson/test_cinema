package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.review.ReviewCreateRequest;
import com.cinema.testcinema.dto.review.ReviewResponse;
import com.cinema.testcinema.dto.review.ReviewUpdateRequest;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.Review;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.ReviewRepository;
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
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public ReviewService(ReviewRepository reviewRepository,
                         MovieRepository movieRepository,
                         UserRepository userRepository,
                         AuthenticatedUserService authenticatedUserService) {
        this.reviewRepository = reviewRepository;
        this.movieRepository = movieRepository;
        this.userRepository = userRepository;
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

        Review parent = null;
        if (request.parentId() != null) {
            parent = reviewRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Родительский отзыв с ID " + request.parentId() + " не найден"));
            if (!Objects.equals(parent.getMovieId(), movie.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Родительский отзыв принадлежит другому фильму");
            }
            long repliesCount = reviewRepository.countByParentId(parent.getId());
            if (repliesCount >= 5) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Превышен лимит ответов для выбранного отзыва");
            }
        }

        Review review = new Review();
        review.setMovie(movie);
        review.setUser(user);
        review.setParent(parent);
        review.setContent(request.content().trim());

        Review saved = reviewRepository.save(review);
        return toResponse(saved, List.of());
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> listByMovie(Long movieId, Pageable pageable) {
        ensureMovieExists(movieId);
        Page<Review> roots = reviewRepository.findByMovieIdAndParentIdIsNull(movieId, pageable);
        List<Long> rootIds = roots.stream().map(Review::getId).toList();
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
        if (!isAdmin && !Objects.equals(review.getUserId(), currentUserId)) {
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
        if (!isAdmin && !Objects.equals(review.getUserId(), currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        reviewRepository.delete(review);
    }

    private Map<Long, List<ReviewResponse>> loadReplies(List<Long> parentIds) {
        if (parentIds.isEmpty()) {
            return Map.of();
        }
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

    private ReviewResponse toResponse(Review review, List<ReviewResponse> replies) {
        ReviewResponse response = new ReviewResponse(
                review.getId(),
                review.getUserId(),
                review.getMovieId(),
                review.getParentId(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                review.isEdited()
        );
        response.setReplies(replies);
        return response;
    }
}
