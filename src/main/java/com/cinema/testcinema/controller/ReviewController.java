package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.ReviewCreateDto;
import com.cinema.testcinema.dto.ReviewDto;
import com.cinema.testcinema.service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies/{imdbId}/reviews")
public class ReviewController {
    private final ReviewService service;

    public ReviewController(ReviewService s){ this.service = s; }

    @GetMapping
    public Page<ReviewDto> list(@PathVariable String imdbId,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size){
        return service.list(imdbId, PageRequest.of(page, size));
    }

    @PostMapping
    public ResponseEntity<ReviewDto> create(@PathVariable String imdbId,
                                            @RequestBody ReviewCreateDto body){
        return ResponseEntity.ok(service.create(imdbId, body));
    }
}
