package com.cinema.testcinema.controller;

import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.model.Watchlist;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.UserRepository;
import com.cinema.testcinema.repository.WatchlistRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/watchlists")
public class WatchlistController {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    public WatchlistController(WatchlistRepository watchlistRepository,
                               UserRepository userRepository,
                               MovieRepository movieRepository) {
        this.watchlistRepository = watchlistRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
    }

    @GetMapping
    public List<WatchlistResponse> getAll() {
        return watchlistRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/user/{userId}")
    public List<WatchlistResponse> getByUser(@PathVariable Long userId) {
        return watchlistRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{userId}/{movieId}")
    public WatchlistResponse getOne(@PathVariable Long userId, @PathVariable Long movieId) {
        Watchlist watchlist = watchlistRepository.findById(new Watchlist.WatchlistId(userId, movieId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Фильм не найден в списке пользователя"));
        return toResponse(watchlist);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WatchlistResponse create(@RequestBody WatchlistRequest request) {
        Long userId = requireUserId(request.userId());
        Long movieId = requireMovieId(request.movieId());

        if (watchlistRepository.existsByUserIdAndMovieId(userId, movieId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Фильм уже есть в списке пользователя");
        }

        User user = loadUser(userId);
        Movie movie = loadMovie(movieId);

        Watchlist watchlist = new Watchlist();
        watchlist.setUser(user);
        watchlist.setMovie(movie);
        Instant addedAt = Optional.ofNullable(request.addedAt()).orElse(Instant.now());
        watchlist.setAddedAt(addedAt);

        Watchlist saved = watchlistRepository.save(watchlist);
        return toResponse(saved);
    }

    @PutMapping("/{userId}/{movieId}")
    public WatchlistResponse update(@PathVariable Long userId,
                                    @PathVariable Long movieId,
                                    @RequestBody WatchlistRequest request) {
        Watchlist watchlist = watchlistRepository.findById(new Watchlist.WatchlistId(userId, movieId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Фильм не найден в списке пользователя"));

        if (request.addedAt() != null) {
            watchlist.setAddedAt(request.addedAt());
        }

        if (request.userId() != null && !request.userId().equals(userId)) {
            Long newUserId = request.userId();
            if (watchlistRepository.existsByUserIdAndMovieId(newUserId, movieId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Указанный пользователь уже имеет этот фильм в списке");
            }
            watchlist.setUser(loadUser(newUserId));
        }

        if (request.movieId() != null && !request.movieId().equals(movieId)) {
            Long newMovieId = request.movieId();
            Long effectiveUserId = request.userId() != null ? request.userId() : watchlist.getUser().getId();
            if (watchlistRepository.existsByUserIdAndMovieId(effectiveUserId, newMovieId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Фильм уже есть в списке пользователя");
            }
            watchlist.setMovie(loadMovie(newMovieId));
        }

        Watchlist saved = watchlistRepository.save(watchlist);
        return toResponse(saved);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam Long userId, @RequestParam Long movieId) {
        if (!watchlistRepository.existsByUserIdAndMovieId(userId, movieId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Запись не найдена в списке пользователя");
        }
        watchlistRepository.deleteByUserIdAndMovieId(userId, movieId);
    }

    private WatchlistResponse toResponse(Watchlist watchlist) {
        return new WatchlistResponse(
                watchlist.getUser() != null ? watchlist.getUser().getId() : null,
                watchlist.getMovie() != null ? watchlist.getMovie().getId() : null,
                watchlist.getAddedAt()
        );
    }

    private Long requireUserId(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не указан userId");
        }
        return userId;
    }

    private Long requireMovieId(Long movieId) {
        if (movieId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не указан movieId");
        }
        return movieId;
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Пользователь с ID " + userId + " не найден"));
    }

    private Movie loadMovie(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Фильм с ID " + movieId + " не найден"));
    }

    public record WatchlistRequest(Long userId, Long movieId, Instant addedAt) {}

    public record WatchlistResponse(Long userId, Long movieId, Instant addedAt) {}
}