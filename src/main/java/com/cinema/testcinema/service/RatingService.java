package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.rating.RatingRequest;
import com.cinema.testcinema.dto.rating.RatingResponse;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.Rating;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.RatingRepository;
import com.cinema.testcinema.repository.UserRepository;
import com.cinema.testcinema.security.AuthenticatedUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Objects;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public RatingService(RatingRepository ratingRepository,
                         MovieRepository movieRepository,
                         UserRepository userRepository,
                         AuthenticatedUserService authenticatedUserService) {
        this.ratingRepository = ratingRepository;
        this.movieRepository = movieRepository;
        this.userRepository = userRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Transactional
    public RatingUpsertResult createOrUpdate(RatingRequest request, Authentication authentication) {
        Long currentUserId = authenticatedUserService.requireCurrentUserId(authentication);
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Пользователь с ID " + currentUserId + " не найден"));

        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Фильм с ID " + request.movieId() + " не найден"));

        Rating rating = ratingRepository.findByUserIdAndMovieId(user.getId(), movie.getId()).orElse(null);
        boolean created = false;
        if (rating == null) {
            rating = new Rating(user, movie, request.score());
            rating.setCreatedAt(Instant.now());
            created = true;
        } else {
            rating.setScore(request.score());
            rating.setMovie(movie);
        }

        Rating saved = ratingRepository.save(rating);
        return new RatingUpsertResult(toResponse(saved), created);
    }

    @Transactional(readOnly = true)
    public RatingResponse getById(Long id, Authentication authentication) {
        authenticatedUserService.requireCurrentUserId(authentication);
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Рейтинг с ID " + id + " не найден"));
        return toResponse(rating);
    }

    @Transactional(readOnly = true)
    public Page<RatingResponse> listByMovie(Long movieId, Pageable pageable, Authentication authentication) {
        authenticatedUserService.requireCurrentUserId(authentication);
        if (!movieRepository.existsById(movieId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Фильм с ID " + movieId + " не найден");
        }
        return ratingRepository.findByMovieId(movieId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public RatingResponse update(Long id, RatingRequest request, Authentication authentication) {
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Рейтинг с ID " + id + " не найден"));

        Long currentUserId = authenticatedUserService.requireCurrentUserId(authentication);
        boolean isAdmin = authenticatedUserService.hasRole(authentication, "ADMIN");
        if (!isAdmin && !Objects.equals(rating.getUser().getId(), currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Фильм с ID " + request.movieId() + " не найден"));

        ratingRepository.findByUserIdAndMovieId(rating.getUser().getId(), movie.getId())
                .filter(existing -> !existing.getId().equals(rating.getId()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Пользователь уже поставил оценку этому фильму");
                });

        rating.setMovie(movie);
        rating.setScore(request.score());

        Rating saved = ratingRepository.save(rating);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, Authentication authentication) {
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Рейтинг с ID " + id + " не найден"));

        Long currentUserId = authenticatedUserService.requireCurrentUserId(authentication);
        boolean isAdmin = authenticatedUserService.hasRole(authentication, "ADMIN");
        if (!isAdmin && !Objects.equals(rating.getUser().getId(), currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        ratingRepository.delete(rating);
    }

    private RatingResponse toResponse(Rating rating) {
        Long userId = rating.getUser() != null ? rating.getUser().getId() : null;
        Long movieId = rating.getMovie() != null ? rating.getMovie().getId() : null;
        return new RatingResponse(
                rating.getId(),
                userId,
                movieId,
                rating.getScore(),
                rating.getCreatedAt()
        );
    }

    public record RatingUpsertResult(RatingResponse response, boolean created) {
    }
}
