package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.ReviewCreateDto;
import com.cinema.testcinema.dto.ReviewDto;
import com.cinema.testcinema.model.Review;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.ReviewRepository;
import com.cinema.testcinema.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    private final ReviewRepository reviews;
    private final MovieRepository movies;
    private final UserRepository users;

    public ReviewService(ReviewRepository r, MovieRepository m, UserRepository u){
        this.reviews = r; this.movies = m; this.users = u;
    }

    @Transactional
    public ReviewDto create(String imdbId, ReviewCreateDto dto){
        var userId = currentUserId();
        var movie = movies.findByImdbId(imdbId).orElseThrow();
        reviews.findByMovieIdAndUserId(movie.getId(), userId)
                .ifPresent(x -> { throw new IllegalStateException("already_exists"); });

        var r = new Review();
        r.setMovie(movie);
        r.setUser(users.findById(userId).orElseThrow());
        r.setBody(req(dto.body(), 1, 10_000));

        var saved = reviews.save(r);
        return map(saved);
    }

    public Page<ReviewDto> list(String imdbId, Pageable p){
        return reviews.findByMovieImdbIdOrderByCreatedAtDesc(imdbId, p).map(this::map);
    }

    private ReviewDto map(Review r){
        return new ReviewDto(
                r.getId(),
                r.getMovie().getImdbId(),
                r.getMovie().getId(),
                r.getUser().getId(),
                r.getUser().getUsername(),
                r.getBody(),
                r.getCreatedAt()
        );
    }

    private static String req(String s, int min, int max){
        if (s == null) throw new IllegalArgumentException("body_required");
        var t = s.trim();
        if (t.length() < min || t.length() > max) throw new IllegalArgumentException("body_size");
        return t;
    }

    private Long currentUserId(){
        var a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof UserDetails ud))
            throw new org.springframework.security.access.AccessDeniedException("unauthorized");
        var username = ud.getUsername();
        return users.findByUsername(username)
                .map(u -> u.getId())
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("user_not_found"));
    }
}
