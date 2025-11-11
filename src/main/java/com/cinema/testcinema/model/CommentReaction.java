package com.cinema.testcinema.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "comment_reactions")
@IdClass(CommentReaction.Key.class)
public class CommentReaction {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @Convert(converter = ReactionConverter.class)
    @Column(name = "reaction", nullable = false)
    private Reaction reaction;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // key
    public static class Key implements Serializable {
        private Long comment;
        private Long user;
        private Reaction reaction;
        public Key(){}
        public Key(Long comment, Long user, Reaction reaction){
            this.comment = comment; this.user = user; this.reaction = reaction;
        }
        @Override public boolean equals(Object o){
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return Objects.equals(comment, k.comment) &&
                    Objects.equals(user, k.user) &&
                    reaction == k.reaction;
        }
        @Override public int hashCode(){ return Objects.hash(comment, user, reaction); }
    }

    // getters/setters
    public Comment getComment(){ return comment; }
    public void setComment(Comment comment){ this.comment = comment; }
    public User getUser(){ return user; }
    public void setUser(User user){ this.user = user; }
    public Reaction getReaction(){ return reaction; }
    public void setReaction(Reaction reaction){ this.reaction = reaction; }
    public Instant getCreatedAt(){ return createdAt; }
}
