package com.cinema.testcinema.controller;

import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.Rating;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.RatingRepository;
import com.cinema.testcinema.repository.UserRepository;
import com.cinema.testcinema.security.AuthenticatedUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ratings")
@PreAuthorize("isAuthenticated()") // страховка: любые методы рейтингов требуют токен
public class RatingController {

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public RatingController(RatingRepository ratingRepository,
                            UserRepository userRepository,
                            MovieRepository movieRepository,
                            AuthenticatedUserService authenticatedUserService) {
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    public List<RatingResponse> getAll() {
        return ratingRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public RatingResponse getById(@PathVariable Long id) {
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Рейтинг с ID " + id + " не найден"));
        return toResponse(rating);
    }

    @GetMapping("/movie/{movieId}")
    public List<RatingResponse> getByMovie(@PathVariable Long movieId) {
        return ratingRepository.findByMovieId(movieId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RatingResponse create(@Valid @RequestBody RatingRequest request, Authentication authentication) {
        Short score = validateScore(request.score());
        User user = loadUser(request.userId());
        Movie movie = loadMovie(request.movieId());

        authenticatedUserService.assertSameUserOrAdmin(authentication, user.getId());

        if (ratingRepository.existsByUserIdAndMovieId(user.getId(), movie.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Пользователь уже поставил оценку этому фильму");
        }

        Rating rating = new Rating();
        rating.setUser(user);
        rating.setMovie(movie);
        rating.setScore(score);
        rating.setComment(request.comment());
        Instant createdAt = Optional.ofNullable(request.createdAt()).orElse(Instant.now());
        rating.setCreatedAt(createdAt);

        Rating saved = ratingRepository.save(rating);
        return toResponse(saved);
    }

    @PutMapping("/{id}")
    public RatingResponse update(@PathVariable Long id,
                                 @Valid @RequestBody RatingRequest request,
                                 Authentication authentication) {
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Рейтинг с ID " + id + " не найден"));

        authenticatedUserService.assertSameUserOrAdmin(authentication, rating.getUser().getId());

        if (request.userId() != null && !request.userId().equals(rating.getUser().getId())) {
            if (!authenticatedUserService.hasRole(authentication, "ADMIN")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
            User newUser = loadUser(request.userId());
            if (request.movieId() == null) {
                if (ratingRepository.existsByUserIdAndMovieId(newUser.getId(), rating.getMovie().getId()) &&
                        !newUser.getId().equals(rating.getUser().getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Нельзя изменить пользователя на того, кто уже поставил оценку этому фильму");
                }
            }
            rating.setUser(newUser);
        }

        if (request.movieId() != null && !request.movieId().equals(rating.getMovie().getId())) {
            Movie newMovie = loadMovie(request.movieId());
            if (request.userId() == null) {
                if (ratingRepository.existsByUserIdAndMovieId(rating.getUser().getId(), newMovie.getId()) &&
                        !newMovie.getId().equals(rating.getMovie().getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Пользователь уже поставил оценку выбранному фильму");
                }
            } else {
                Long effectiveUserId = request.userId();
                Rating existing = ratingRepository.findByUserIdAndMovieId(effectiveUserId, newMovie.getId()).orElse(null);
                if (existing != null && !existing.getId().equals(rating.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Пользователь уже поставил оценку выбранному фильму");
                }
            }
            rating.setMovie(newMovie);
        }

        if (request.score() != null) {
            rating.setScore(validateScore(request.score()));
        }

        rating.setComment(request.comment());
        if (request.createdAt() != null) {
            rating.setCreatedAt(request.createdAt());
        }

        Rating saved = ratingRepository.save(rating);
        return toResponse(saved);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Рейтинг с ID " + id + " не найден"));
        authenticatedUserService.assertSameUserOrAdmin(authentication, rating.getUser().getId());
        ratingRepository.delete(rating);
    }

    private RatingResponse toResponse(Rating rating) {
        return new RatingResponse(
                rating.getId(),
                rating.getUser() != null ? rating.getUser().getId() : null,
                rating.getMovie() != null ? rating.getMovie().getId() : null,
                rating.getScore(),
                rating.getComment(),
                rating.getCreatedAt()
        );
    }

    private User loadUser(Long userId) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не указан userId");
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Пользователь с ID " + userId + " не найден"));
    }

    private Movie loadMovie(Long movieId) {
        if (movieId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не указан movieId");
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Фильм с ID " + movieId + " не найден"));
    }

    private short validateScore(Short score) {
        if (score == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не указана оценка (score)");
        if (score < 1 || score > 10)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Оценка должна быть в диапазоне от 1 до 10");
        return score;
    }

    public record RatingRequest(Long userId, Long movieId, Short score, String comment, Instant createdAt) {}
    public record RatingResponse(Long id, Long userId, Long movieId, Short score, String comment, Instant createdAt) {}
}
