package com.cinema.testcinema.controller;

import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.model.Watchlist;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.UserRepository;
import com.cinema.testcinema.repository.WatchlistRepository;
import com.cinema.testcinema.security.AuthenticatedUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
    private final AuthenticatedUserService authenticatedUserService;

    public WatchlistController(WatchlistRepository watchlistRepository,
                               UserRepository userRepository,
                               MovieRepository movieRepository,
                               AuthenticatedUserService authenticatedUserService) {
        this.watchlistRepository = watchlistRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    public List<WatchlistResponse> getAll() {
        return watchlistRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/user/{userId}")
    public List<WatchlistResponse> getByUser(@PathVariable Long userId,
                                             Authentication authentication) {
        authenticatedUserService.assertSameUserOrAdmin(authentication, userId);
        return watchlistRepository.findByUser_Id(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{userId}/{movieId}")
    public WatchlistResponse getOne(@PathVariable Long userId,
                                    @PathVariable Long movieId,
                                    Authentication authentication) {
        authenticatedUserService.assertSameUserOrAdmin(authentication, userId);
        Watchlist watchlist = watchlistRepository
                .findById(new Watchlist.WatchlistId(userId, movieId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
        return toResponse(watchlist);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WatchlistResponse create(@Valid @RequestBody WatchlistRequest request,
                                    Authentication authentication) {
        Long userId = requireUserId(request.userId());
        Long movieId = requireMovieId(request.movieId());

        authenticatedUserService.assertSameUserOrAdmin(authentication, userId);

        if (watchlistRepository.existsByUser_IdAndMovie_Id(userId, movieId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already exists");
        }

        User user = loadUser(userId);
        Movie movie = loadMovie(movieId);

        Watchlist watchlist = new Watchlist();
        watchlist.setUser(user);
        watchlist.setMovie(movie);
        watchlist.setAddedAt(Optional.ofNullable(request.addedAt()).orElse(Instant.now()));

        return toResponse(watchlistRepository.save(watchlist));
    }

    @PutMapping("/{userId}/{movieId}")
    public WatchlistResponse update(@PathVariable Long userId,
                                    @PathVariable Long movieId,
                                    @Valid @RequestBody WatchlistRequest request,
                                    Authentication authentication) {
        authenticatedUserService.assertSameUserOrAdmin(authentication, userId);

        Watchlist watchlist = watchlistRepository
                .findById(new Watchlist.WatchlistId(userId, movieId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

        if (request.addedAt() != null) {
            watchlist.setAddedAt(request.addedAt());
        }

        if (request.userId() != null && !request.userId().equals(userId)) {
            if (!authenticatedUserService.hasRole(authentication, "ADMIN")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
            Long newUserId = request.userId();
            if (watchlistRepository.existsByUser_IdAndMovie_Id(newUserId, movieId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Exists");
            }
            watchlist.setUser(loadUser(newUserId));
        }

        if (request.movieId() != null && !request.movieId().equals(movieId)) {
            Long newMovieId = request.movieId();
            Long effectiveUserId = request.userId() != null ? request.userId() : watchlist.getUser().getId();
            if (watchlistRepository.existsByUser_IdAndMovie_Id(effectiveUserId, newMovieId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Exists");
            }
            watchlist.setMovie(loadMovie(newMovieId));
        }

        return toResponse(watchlistRepository.save(watchlist));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam Long userId,
                       @RequestParam Long movieId,
                       Authentication authentication) {
        authenticatedUserService.assertSameUserOrAdmin(authentication, userId);

        if (!watchlistRepository.existsByUser_IdAndMovie_Id(userId, movieId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        }

        watchlistRepository.deleteByUser_IdAndMovie_Id(userId, movieId);
    }

    private WatchlistResponse toResponse(Watchlist watchlist) {
        return new WatchlistResponse(
                watchlist.getUser() != null ? watchlist.getUser().getId() : null,
                watchlist.getMovie() != null ? watchlist.getMovie().getId() : null,
                watchlist.getAddedAt()
        );
    }

    private Long requireUserId(Long userId) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId missing");
        return userId;
    }

    private Long requireMovieId(Long movieId) {
        if (movieId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "movieId missing");
        return movieId;
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Movie loadMovie(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));
    }

    public record WatchlistRequest(Long userId, Long movieId, Instant addedAt) {}

    public record WatchlistResponse(Long userId, Long movieId, Instant addedAt) {}
}
