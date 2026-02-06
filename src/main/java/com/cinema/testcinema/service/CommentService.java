package com.cinema.testcinema.service;

import com.cinema.testcinema.dto.comment.CommentCreateRequest;
import com.cinema.testcinema.dto.comment.CommentCountResponse;
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
import com.cinema.testcinema.repository.UserProfileRepository;
import com.cinema.testcinema.repository.UserRepository;
import com.cinema.testcinema.security.AuthenticatedUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public CommentService(CommentRepository commentRepository,
                          CommentReactionRepository commentReactionRepository,
                          MovieRepository movieRepository,
                          UserRepository userRepository,
                          UserProfileRepository userProfileRepository,
                          AuthenticatedUserService authenticatedUserService) {
        this.commentRepository = commentRepository;
        this.commentReactionRepository = commentReactionRepository;
        this.movieRepository = movieRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    /**
     * Корневые комментарии по фильму с сортировкой:
     * order=new | old | top
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> listByMovie(Long movieId, Pageable pageable, String order) {
        ensureMovieExists(movieId);

        Pageable effectivePageable = normalizeOrder(pageable, order);

        Page<Comment> roots;
        if ("top".equalsIgnoreCase(order)) {
            roots = commentRepository.findRootsByMovieIdOrderByPopularity(movieId, effectivePageable);
        } else {
            roots = commentRepository.findByMovieIdAndParentIdIsNull(movieId, effectivePageable);
        }

        List<Long> rootIds = roots.stream().map(Comment::getId).toList();
        if (rootIds.isEmpty()) {
            return new PageImpl<>(List.of(), effectivePageable, roots.getTotalElements());
        }

        // прямые ответы для root-комментов
        List<Comment> replies = commentRepository.findByParentIdIn(rootIds);
        Map<Long, List<Comment>> repliesMap = replies.stream()
                .collect(Collectors.groupingBy(Comment::getParentId));

        // ответы сортируем хронологически как диалог
        repliesMap.values().forEach(list -> list.sort(Comparator.comparing(Comment::getCreatedAt)));

        // реакции для root + replies
        Set<Long> allIds = new HashSet<>(rootIds);
        allIds.addAll(replies.stream().map(Comment::getId).toList());
        Map<Long, List<CommentReactionSummary>> reactionMap = buildReactionMap(allIds);

        return roots.map(root -> toResponse(root, repliesMap.getOrDefault(root.getId(), List.of()), reactionMap));
    }

    /**
     * Счётчики комментариев по фильму
     */
    @Transactional(readOnly = true)
    public CommentCountResponse getCountsByMovie(Long movieId) {
        ensureMovieExists(movieId);
        long total = commentRepository.countByMovieId(movieId);
        long roots = commentRepository.countByMovieIdAndParentIdIsNull(movieId);
        return new CommentCountResponse(roots, total);
    }

    @Transactional(readOnly = true)
    public CommentResponse getById(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Комментарий не найден"));

        // Page.getContent() -> unmodifiable list, поэтому делаем копию
        List<Comment> replies = new ArrayList<>(
                commentRepository.findByParentId(id, Pageable.unpaged()).getContent()
        );
        replies.sort(Comparator.comparing(Comment::getCreatedAt));

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

            // сравнение по safeMovieId (иначе movieId может быть null из-за insertable=false)
            if (!Objects.equals(safeMovieId(parent), movie.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Родительский комментарий принадлежит другому фильму");
            }
        }

        Comment comment = new Comment();
        comment.setMovie(movie);
        comment.setUser(user);
        comment.setParent(parent);
        comment.setContent(request.content().trim());

        Comment saved = commentRepository.save(comment);

        // реакций пока нет
        return toResponse(saved, List.of(), Map.of());
    }

    @Transactional
    public CommentResponse update(Long id, CommentUpdateRequest request, Authentication auth) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Комментарий не найден"));

        Long userId = authenticatedUserService.requireCurrentUserId(auth);

        if (!Objects.equals(safeUserId(comment), userId)) {
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

        if (!isAdmin && !Objects.equals(safeUserId(comment), userId)) {
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

        CommentReaction existing = commentReactionRepository
                .findByCommentIdAndUserIdAndEmoji(commentId, userId, emoji)
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

    // ---------------------- helpers ----------------------

    private void ensureMovieExists(Long movieId) {
        if (!movieRepository.existsById(movieId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден");
        }
    }

    private Pageable normalizeOrder(Pageable pageable, String order) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();

        if ("top".equalsIgnoreCase(order)) {
            // сортировка внутри query
            return PageRequest.of(page, size);
        }

        if ("old".equalsIgnoreCase(order)) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        }

        // default = new
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Ключевой фикс: НЕ использовать comment.getUserId()/getMovieId() сразу после save(),
     * потому что это read-only поля (insertable=false/updatable=false) и могут быть null.
     */
    private Long safeUserId(Comment c) {
        if (c.getUser() != null) return c.getUser().getId();
        return c.getUserId();
    }

    private Long safeMovieId(Comment c) {
        if (c.getMovie() != null) return c.getMovie().getId();
        return c.getMovieId();
    }

    private CommentResponse toResponse(Comment comment, List<Comment> replies,
                                       Map<Long, List<CommentReactionSummary>> reactionMap) {

        Long userId = safeUserId(comment);
        Long movieId = safeMovieId(comment);

        CommentResponse response = new CommentResponse(
                comment.getId(),
                userId,
                movieId,
                comment.getParentId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                comment.isEdited()
        );

        // replies -> responses
        List<CommentResponse> replyResponses = replies.stream()
                .map(r -> toResponse(r, List.of(), reactionMap))
                .collect(Collectors.toCollection(ArrayList::new));

        response.setReplies(replyResponses);
        response.setRepliesCount(replyResponses.size());

        List<CommentReactionSummary> reactions = reactionMap.getOrDefault(comment.getId(), List.of());
        response.setReactions(reactions);

        long totalReactions = reactions.stream().mapToLong(CommentReactionSummary::count).sum();
        response.setTotalReactions(totalReactions);

        // автор
        if (userId != null) {
            userRepository.findById(userId).ifPresent(u -> {
                response.setAuthorUsername(u.getUsername());
                userProfileRepository.findByUserId(u.getId())
                        .ifPresent(p -> response.setAuthorAvatarUrl(p.getAvatarUrl()));
            });
        }

        return response;
    }

    private Map<Long, List<CommentReactionSummary>> buildReactionMap(Set<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return Map.of();
        }

        List<CommentReaction> reactions = commentReactionRepository.findByCommentIdIn(commentIds);

        Map<Long, Map<String, Long>> counts = new HashMap<>();
        for (CommentReaction reaction : reactions) {
            Long cId = reaction.getComment().getId();
            counts.computeIfAbsent(cId, x -> new HashMap<>())
                    .merge(reaction.getEmoji(), 1L, Long::sum);
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
        if (reactions == null || reactions.isEmpty()) return List.of();

        Map<String, Long> counts = new HashMap<>();
        for (CommentReaction r : reactions) {
            counts.merge(r.getEmoji(), 1L, Long::sum);
        }

        return counts.entrySet().stream()
                .map(e -> new CommentReactionSummary(e.getKey(), e.getValue()))
                .toList();
    }
}
