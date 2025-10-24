package com.cinema.testcinema.controller;

import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.Review;
import com.cinema.testcinema.service.MovieService;
import com.cinema.testcinema.service.RedisReviewCacheService;
import com.cinema.testcinema.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
public class ReviewController {

    private final ReviewService reviewService;
    private final RedisReviewCacheService cacheService;
    private final MovieService movieService;

    public ReviewController(ReviewService reviewService,
                            RedisReviewCacheService cacheService,
                            MovieService movieService) {
        this.reviewService = reviewService;
        this.cacheService = cacheService;
        this.movieService = movieService;
    }

    @PostMapping("/movies/{movieId}/reviews")
    public Review addReview(@PathVariable Long movieId, @RequestBody Review review) {
        Movie movie = movieService.getMovieById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));
        review.setMovie(movie);
        Review saved = reviewService.addReview(review);
        cacheService.saveReviewText(saved.getId(), saved.getComment());
        return saved;
    }

    @GetMapping("/api/movies/{movieId}/reviews")
    public List<Review> getReviewsByMovie(@PathVariable Long movieId) {
        return reviewService.getReviewsByMovieId(movieId);
    }

    @GetMapping("/reviews/{id}/cached")
    public String getCachedReview(@PathVariable Long id) {
        return cacheService.getReviewText(id);
    }
}
