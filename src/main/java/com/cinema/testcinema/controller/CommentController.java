package com.cinema.testcinema.controller;

import com.cinema.testcinema.dto.comment.CommentCreateRequest;
import com.cinema.testcinema.dto.comment.CommentReactionRequest;
import com.cinema.testcinema.dto.comment.CommentReactionSummary;
import com.cinema.testcinema.dto.comment.CommentResponse;
import com.cinema.testcinema.dto.comment.CommentUpdateRequest;
import com.cinema.testcinema.service.CommentService;
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

import java.util.List;

@RestController
@RequestMapping("/comments")
@Tag(name = "Comments", description = "Комментарии к фильмам")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/movie/{movieId}")
    @Operation(summary = "Получить корневые комментарии по фильму")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Страница комментариев",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CommentResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Фильм не найден")
    })
    public Page<CommentResponse> getByMovie(@PathVariable Long movieId,
                                            @Parameter(description = "Параметры пагинации")
                                            @PageableDefault(size = 20) Pageable pageable) {
        return commentService.listByMovie(movieId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить комментарий с прямыми ответами")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Комментарий найден",
                    content = @Content(schema = @Schema(implementation = CommentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Комментарий не найден")
    })
    public CommentResponse getById(@PathVariable Long id) {
        return commentService.getById(id);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Создать комментарий или ответ")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Комментарий создан",
                    content = @Content(schema = @Schema(implementation = CommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Необходима авторизация"),
            @ApiResponse(responseCode = "404", description = "Фильм или комментарий не найдены")
    })
    public ResponseEntity<CommentResponse> create(@Valid @RequestBody CommentCreateRequest request,
                                                  Authentication authentication) {
        CommentResponse response = commentService.create(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Обновить комментарий")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Комментарий обновлен",
                    content = @Content(schema = @Schema(implementation = CommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "401", description = "Необходима авторизация"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Комментарий не найден")
    })
    public CommentResponse update(@PathVariable Long id,
                                  @Valid @RequestBody CommentUpdateRequest request,
                                  Authentication authentication) {
        return commentService.update(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить комментарий вместе с потомками")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Комментарий удален"),
            @ApiResponse(responseCode = "401", description = "Необходима авторизация"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Комментарий не найден")
    })
    public void delete(@PathVariable Long id, Authentication authentication) {
        commentService.delete(id, authentication);
    }

    @PostMapping("/{id}/reactions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Поставить/снять реакцию на комментарий")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список реакций",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CommentReactionSummary.class)))),
            @ApiResponse(responseCode = "401", description = "Необходима авторизация"),
            @ApiResponse(responseCode = "404", description = "Комментарий не найден")
    })
    public List<CommentReactionSummary> react(@PathVariable Long id,
                                              @Valid @RequestBody CommentReactionRequest request,
                                              Authentication authentication) {
        return commentService.toggleReaction(id, request.emoji(), authentication);
    }
}
