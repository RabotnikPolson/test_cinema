package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.CommentReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {
    List<CommentReaction> findByCommentIdIn(Collection<Long> commentIds);

    List<CommentReaction> findByCommentId(Long commentId);

    Optional<CommentReaction> findByCommentIdAndUserIdAndEmoji(Long commentId, Long userId, String emoji);
}
