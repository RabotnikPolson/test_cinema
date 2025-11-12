package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.review.ReviewCreateRequest;
import com.cinema.testcinema.dto.review.ReviewResponse;
import com.cinema.testcinema.dto.review.ReviewUpdateRequest;
import com.cinema.testcinema.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reviews")
@Tag(name = "Reviews", description = "Отзывы и ответы на фильмы")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/movie/{movieId}")
    @Operation(summary = "Получить корневые отзывы по фильму")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Страница корневых отзывов",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReviewResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Фильм не найден")
    })
    public Page<ReviewResponse> getByMovie(@PathVariable Long movieId,
                                           @Parameter(description = "Параметры пагинации")
                                           @PageableDefault(size = 20) Pageable pageable) {
        return reviewService.listByMovie(movieId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить отзыв с прямыми ответами")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Отзыв найден",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "404", description = "Отзыв не найден")
    })
    public ReviewResponse getById(@PathVariable Long id) {
        return reviewService.get(id);
    }

    @GetMapping("/{id}/replies")
    @Operation(summary = "Получить прямые ответы на отзыв")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Страница ответов",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReviewResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Отзыв не найден")
    })
    public Page<ReviewResponse> getReplies(@PathVariable Long id,
                                           @Parameter(description = "Параметры пагинации")
                                           @PageableDefault(size = 20) Pageable pageable) {
        return reviewService.getReplies(id, pageable);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Создать отзыв или ответ")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Отзыв создан",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Необходима авторизация"),
            @ApiResponse(responseCode = "404", description = "Фильм или родительский отзыв не найдены"),
            @ApiResponse(responseCode = "409", description = "Превышен лимит ответов")
    })
    public ResponseEntity<ReviewResponse> create(@Valid @RequestBody ReviewCreateRequest request,
                                                 Authentication authentication) {
        ReviewResponse response = reviewService.create(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Обновить отзыв в течение часа после создания")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Отзыв обновлён",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Необходима авторизация"),
            @ApiResponse(responseCode = "403", description = "Редактирование недоступно"),
            @ApiResponse(responseCode = "404", description = "Отзыв не найден")
    })
    public ReviewResponse update(@PathVariable Long id,
                                 @Valid @RequestBody ReviewUpdateRequest request,
                                 Authentication authentication) {
        return reviewService.update(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить отзыв вместе с дочерними ответами")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Отзыв удалён"),
            @ApiResponse(responseCode = "401", description = "Необходима авторизация"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Отзыв не найден")
    })
    public void delete(@PathVariable Long id, Authentication authentication) {
        reviewService.delete(id, authentication);
    }
}
