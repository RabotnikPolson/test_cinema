package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.CommentCreateDto;
import com.cinema.testcinema.dto.CommentDto;
import com.cinema.testcinema.model.Comment;
import com.cinema.testcinema.model.CommentReaction;
import com.cinema.testcinema.model.Reaction;
import com.cinema.testcinema.repository.CommentReactionRepository;
import com.cinema.testcinema.repository.CommentRepository;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.ReviewRepository;
import com.cinema.testcinema.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CommentService {
    private final CommentRepository comments;
    private final MovieRepository movies;
    private final ReviewRepository reviews;
    private final UserRepository users;
    private final CommentReactionRepository reactions;

    public CommentService(CommentRepository c, MovieRepository m, ReviewRepository r,
                          UserRepository u, CommentReactionRepository cr){
        this.comments = c; this.movies = m; this.reviews = r; this.users = u; this.reactions = cr;
    }

    @Transactional
    public CommentDto create(CommentCreateDto dto){
        var userId = currentUserId();
        var user = users.findById(userId).orElseThrow();
        var movie = movies.findByImdbId(dto.imdbId()).orElseThrow();

        var c = new Comment();
        c.setMovie(movie);
        if (dto.reviewId() != null) c.setReview(reviews.findById(dto.reviewId()).orElseThrow());
        if (dto.parentId() != null) c.setParent(comments.findById(dto.parentId()).orElseThrow());
        c.setUser(user);
        c.setBody(req(dto.body(), 1, 5000));

        var saved = comments.save(c);
        return enrich(saved);
    }

    public Page<CommentDto> listMovie(String imdbId, Pageable p){
        return comments.findByMovieImdbIdAndReviewIsNullOrderByCreatedAtDesc(imdbId, p).map(this::enrich);
    }

    public Page<CommentDto> listReview(Long reviewId, Pageable p){
        return comments.findByReviewIdOrderByCreatedAtDesc(reviewId, p).map(this::enrich);
    }

    @Transactional
    public void delete(Long commentId){
        var userId = currentUserId();
        var c = comments.findById(commentId).orElseThrow();
        var caller = users.findById(userId).orElseThrow();
        var isOwner = Objects.equals(c.getUser().getId(), userId);
        var isAdmin = caller.getRole() != null && caller.getRole().equalsIgnoreCase("ADMIN");
        if (!isOwner && !isAdmin) throw new org.springframework.security.access.AccessDeniedException("forbidden");
        comments.delete(c);
    }

    @Transactional
    public void addReaction(Long commentId, Reaction reaction){
        var userId = currentUserId();
        if (!reactions.existsByCommentIdAndUserIdAndReaction(commentId, userId, reaction)) {
            var cr = new CommentReaction();
            cr.setComment(comments.getReferenceById(commentId));
            cr.setUser(users.getReferenceById(userId));
            cr.setReaction(reaction);
            reactions.save(cr);
        }
    }

    @Transactional
    public void removeReaction(Long commentId, Reaction reaction){
        var userId = currentUserId();
        reactions.deleteByCommentIdAndUserIdAndReaction(commentId, userId, reaction);
    }

    // helpers

    private CommentDto enrich(Comment c){
        var counts = countsOf(List.of(c.getId())).getOrDefault(c.getId(), new long[6]);
        return new CommentDto(
                c.getId(),
                c.getMovie().getImdbId(),
                c.getMovie().getId(),
                c.getReview()==null?null:c.getReview().getId(),
                c.getParent()==null?null:c.getParent().getId(),
                c.getUser().getId(),
                c.getUser().getUsername(),
                c.getBody(),
                counts[0], counts[1], counts[2], counts[3], counts[4], counts[5],
                c.getCreatedAt()
        );
    }

    private Map<Long, long[]> countsOf(List<Long> ids){
        if (ids.isEmpty()) return Map.of();
        var rows = reactions.aggregateByCommentIds(ids);
        var map = new HashMap<Long, long[]>();
        for (var r: rows){
            var arr = map.computeIfAbsent(r.getCommentId(), k -> new long[6]);
            arr[r.getReaction().code - 1] = r.getCnt();
        }
        return map;
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
