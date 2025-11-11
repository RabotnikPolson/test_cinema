package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.CommentCreateDto;
import com.cinema.testcinema.dto.CommentDto;
import com.cinema.testcinema.model.Reaction;
import com.cinema.testcinema.service.CommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CommentController {
    private final CommentService service;

    public CommentController(CommentService s){ this.service = s; }

    // Комментарии к фильму
    @GetMapping("/api/movies/{imdbId}/comments")
    public Page<CommentDto> movieComments(@PathVariable String imdbId,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size){
        return service.listMovie(imdbId, PageRequest.of(page, size));
    }

    @PostMapping("/api/movies/{imdbId}/comments")
    public CommentDto addMovieComment(@PathVariable String imdbId,
                                      @RequestBody CommentCreateDto body){
        // body.imdbId можно игнорировать, берем из path
        return service.create(new CommentCreateDto(imdbId, null, body.parentId(), body.body()));
    }

    // Комментарии под отзывом
    @GetMapping("/api/reviews/{reviewId}/comments")
    public Page<CommentDto> reviewComments(@PathVariable Long reviewId,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size){
        return service.listReview(reviewId, PageRequest.of(page, size));
    }

    @PostMapping("/api/reviews/{reviewId}/comments")
    public CommentDto addReviewComment(@PathVariable Long reviewId,
                                       @RequestBody CommentCreateDto body){
        return service.create(new CommentCreateDto(body.imdbId(), reviewId, body.parentId(), body.body()));
    }

    // Реакции на комментарии
    @PostMapping("/api/comments/{commentId}/reactions/{reaction}")
    public ResponseEntity<Void> addReaction(@PathVariable Long commentId,
                                            @PathVariable String reaction){
        service.addReaction(commentId, Reaction.valueOf(reaction.toUpperCase()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/comments/{commentId}/reactions/{reaction}")
    public ResponseEntity<Void> removeReaction(@PathVariable Long commentId,
                                               @PathVariable String reaction){
        service.removeReaction(commentId, Reaction.valueOf(reaction.toUpperCase()));
        return ResponseEntity.noContent().build();
    }

    // Удаление комментария (владелец или админ)
    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long commentId){
        service.delete(commentId);
        return ResponseEntity.noContent().build();
    }
}
