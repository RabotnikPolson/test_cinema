package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.rating.RatingRequest;
import com.cinema.testcinema.dto.rating.RatingResponse;
import com.cinema.testcinema.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/ratings")
@Tag(name = "Ratings", description = "Управление пользовательскими оценками фильмов")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Получить рейтинг по идентификатору")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Рейтинг найден",
                    content = @Content(schema = @Schema(implementation = RatingResponse.class))),
            @ApiResponse(responseCode = "401", description = "Необходима авторизация"),
            @ApiResponse(responseCode = "404", description = "Рейтинг не найден")
    })
    public RatingResponse getById(@PathVariable Long id, Authentication authentication) {
        return ratingService.getById(id, authentication);
    }

    @GetMapping("/movie/{movieId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Список рейтингов фильма")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список рейтингов",
                    content = @Content(schema = @Schema(implementation = RatingResponse.class))),
            @ApiResponse(responseCode = "401", description = "Необходима авторизация"),
            @ApiResponse(responseCode = "404", description = "Фильм не найден")
    })
    public Page<RatingResponse> getByMovie(@PathVariable Long movieId,
                                           @Parameter(description = "Параметры пагинации")
                                           @PageableDefault(size = 20) Pageable pageable,
                                           Authentication authentication) {
        return ratingService.listByMovie(movieId, pageable, authentication);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Создать или обновить рейтинг",
            description = "Повторный POST обновляет оценку пользователя для фильма")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Рейтинг создан",
                    content = @Content(schema = @Schema(implementation = RatingResponse.class))),
            @ApiResponse(responseCode = "200", description = "Рейтинг обновлён",
                    content = @Content(schema = @Schema(implementation = RatingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Необходима авторизация"),
            @ApiResponse(responseCode = "404", description = "Пользователь или фильм не найдены")
    })
    public ResponseEntity<RatingResponse> create(@Valid @RequestBody RatingRequest request,
                                                 Authentication authentication) {
        RatingService.RatingUpsertResult result = ratingService.createOrUpdate(request, authentication);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.response());
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Обновить существующий рейтинг")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Рейтинг обновлён",
                    content = @Content(schema = @Schema(implementation = RatingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Необходима авторизация"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Рейтинг или фильм не найдены"),
            @ApiResponse(responseCode = "409", description = "Конфликт уникальности")
    })
    public RatingResponse update(@PathVariable Long id,
                                 @Valid @RequestBody RatingRequest request,
                                 Authentication authentication) {
        return ratingService.update(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить рейтинг")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Рейтинг удалён"),
            @ApiResponse(responseCode = "401", description = "Необходима авторизация"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Рейтинг не найден")
    })
    public void delete(@PathVariable Long id, Authentication authentication) {
        ratingService.delete(id, authentication);
    }
}
