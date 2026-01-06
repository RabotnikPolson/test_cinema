package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.comment.CommentCreateRequest;
import com.cinema.testcinema.dto.comment.CommentReactionSummary;
import com.cinema.testcinema.dto.comment.CommentResponse;
import com.cinema.testcinema.dto.comment.CommentUpdateRequest;
import com.cinema.testcinema.model.Comment;
import com.cinema.testcinema.model.CommentReaction;
import com.cinema.testcinema.model.Movie;
import com.cinema.testcinema.model.User;
import com.cinema.testcinema.repository.CommentReactionRepository;
import com.cinema.testcinema.repository.CommentRepository;
import com.cinema.testcinema.repository.MovieRepository;
import com.cinema.testcinema.repository.UserRepository;
import com.cinema.testcinema.security.AuthenticatedUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public CommentService(CommentRepository commentRepository,
                          CommentReactionRepository commentReactionRepository,
                          MovieRepository movieRepository,
                          UserRepository userRepository,
                          AuthenticatedUserService authenticatedUserService) {
        this.commentRepository = commentRepository;
        this.commentReactionRepository = commentReactionRepository;
        this.movieRepository = movieRepository;
        this.userRepository = userRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> listByMovie(Long movieId, Pageable pageable) {
        ensureMovieExists(movieId);
        Page<Comment> roots = commentRepository.findByMovieIdAndParentIdIsNull(movieId, pageable);
        List<Long> rootIds = roots.stream().map(Comment::getId).toList();
        if (rootIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, roots.getTotalElements());
        }

        List<Comment> replies = commentRepository.findByParentIdIn(rootIds);
        Map<Long, List<Comment>> repliesMap = replies.stream()
                .collect(Collectors.groupingBy(Comment::getParentId));

        Set<Long> allIds = new HashSet<>(rootIds);
        allIds.addAll(replies.stream().map(Comment::getId).collect(Collectors.toSet()));
        Map<Long, List<CommentReactionSummary>> reactionMap = buildReactionMap(allIds);

        return roots.map(root -> toResponse(root, repliesMap.getOrDefault(root.getId(), List.of()), reactionMap));
    }

    @Transactional(readOnly = true)
    public CommentResponse getById(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Комментарий не найден"));
        List<Comment> replies = commentRepository.findByParentId(id, Pageable.unpaged()).getContent();

        Set<Long> allIds = new HashSet<>();
        allIds.add(comment.getId());
        allIds.addAll(replies.stream().map(Comment::getId).toList());
        Map<Long, List<CommentReactionSummary>> reactionMap = buildReactionMap(allIds);

        return toResponse(comment, replies, reactionMap);
    }

    @Transactional
    public CommentResponse create(CommentCreateRequest request, Authentication auth) {
        Long userId = authenticatedUserService.requireCurrentUserId(auth);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден"));

        Comment parent = null;
        if (request.parentId() != null) {
            parent = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Родительский комментарий не найден"));
            if (!Objects.equals(parent.getMovieId(), movie.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Родительский комментарий принадлежит другому фильму");
            }
        }

        Comment comment = new Comment();
        comment.setMovie(movie);
        comment.setUser(user);
        comment.setParent(parent);
        comment.setContent(request.content().trim());
        Comment saved = commentRepository.save(comment);
        return toResponse(saved, List.of(), Map.of());
    }

    @Transactional
    public CommentResponse update(Long id, CommentUpdateRequest request, Authentication auth) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Комментарий не найден"));
        Long userId = authenticatedUserService.requireCurrentUserId(auth);
        if (!Objects.equals(comment.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        comment.setContent(request.content().trim());
        comment.setEdited(true);
        Comment saved = commentRepository.save(comment);
        return toResponse(saved, List.of(), buildReactionMap(Set.of(saved.getId())));
    }

    @Transactional
    public void delete(Long id, Authentication auth) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Комментарий не найден"));
        Long userId = authenticatedUserService.requireCurrentUserId(auth);
        boolean isAdmin = authenticatedUserService.hasRole(auth, "ADMIN");
        if (!isAdmin && !Objects.equals(comment.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        commentRepository.delete(comment);
    }

    @Transactional
    public List<CommentReactionSummary> toggleReaction(Long commentId, String emoji, Authentication auth) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Комментарий не найден"));
        Long userId = authenticatedUserService.requireCurrentUserId(auth);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));

        CommentReaction existing = commentReactionRepository.findByCommentIdAndUserIdAndEmoji(commentId, userId, emoji)
                .orElse(null);
        if (existing == null) {
            CommentReaction reaction = new CommentReaction();
            reaction.setComment(comment);
            reaction.setUser(user);
            reaction.setEmoji(emoji);
            commentReactionRepository.save(reaction);
        } else {
            commentReactionRepository.delete(existing);
        }

        List<CommentReaction> reactions = commentReactionRepository.findByCommentId(commentId);
        return summarizeReactions(reactions);
    }

    private void ensureMovieExists(Long movieId) {
        if (!movieRepository.existsById(movieId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден");
        }
    }

    private CommentResponse toResponse(Comment comment, List<Comment> replies,
                                       Map<Long, List<CommentReactionSummary>> reactionMap) {
        CommentResponse response = new CommentResponse(
                comment.getId(),
                comment.getUserId(),
                comment.getMovieId(),
                comment.getParentId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                comment.isEdited()
        );
        List<CommentResponse> replyResponses = replies.stream()
                .map(reply -> toResponse(reply, List.of(), reactionMap))
                .collect(Collectors.toCollection(ArrayList::new));
        response.setReplies(replyResponses);
        response.setReactions(reactionMap.getOrDefault(comment.getId(), List.of()));
        return response;
    }

    private Map<Long, List<CommentReactionSummary>> buildReactionMap(Set<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Map.of();
        }
        List<CommentReaction> reactions = commentReactionRepository.findByCommentIdIn(commentIds);
        Map<Long, Map<String, Long>> counts = new HashMap<>();
        for (CommentReaction reaction : reactions) {
            Long cId = reaction.getComment().getId();
            Map<String, Long> emojiCounts = counts.computeIfAbsent(cId, id -> new HashMap<>());
            emojiCounts.merge(reaction.getEmoji(), 1L, Long::sum);
        }
        Map<Long, List<CommentReactionSummary>> result = new HashMap<>();
        for (Map.Entry<Long, Map<String, Long>> entry : counts.entrySet()) {
            List<CommentReactionSummary> summaries = entry.getValue().entrySet().stream()
                    .map(e -> new CommentReactionSummary(e.getKey(), e.getValue()))
                    .toList();
            result.put(entry.getKey(), summaries);
        }
        return result;
    }

    private List<CommentReactionSummary> summarizeReactions(List<CommentReaction> reactions) {
        if (reactions.isEmpty()) {
            return List.of();
        }
        Map<String, Long> counts = new HashMap<>();
        for (CommentReaction reaction : reactions) {
            counts.merge(reaction.getEmoji(), 1L, Long::sum);
        }
        return counts.entrySet().stream()
                .map(entry -> new CommentReactionSummary(entry.getKey(), entry.getValue()))
                .toList();
    }
}
