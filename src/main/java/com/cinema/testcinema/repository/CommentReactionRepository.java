package com.cinema.testcinema.repository;

import com.cinema.testcinema.model.CommentReaction;
import com.cinema.testcinema.model.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, CommentReaction.Key> {

    boolean existsByCommentIdAndUserIdAndReaction(Long commentId, Long userId, Reaction reaction);

    void deleteByCommentIdAndUserIdAndReaction(Long commentId, Long userId, Reaction reaction);

    @Query("""
        select cr.comment.id as commentId, cr.reaction as reaction, count(cr) as cnt
        from CommentReaction cr
        where cr.comment.id in :ids
        group by cr.comment.id, cr.reaction
    """)
    List<ReactionRow> aggregateByCommentIds(@Param("ids") List<Long> ids);

    interface ReactionRow {
        Long getCommentId();
        Reaction getReaction();
        long getCnt();
    }
}
