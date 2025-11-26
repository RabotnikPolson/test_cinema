package com.cinema.testcinema.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(unique = true)
    private String nickname;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(nullable = false, name = "is_private")
    private boolean isPrivate = false;

    @Column(name = "last_profile_edit_at")
    private Instant lastProfileEditAt;

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public Instant getLastProfileEditAt() {
        return lastProfileEditAt;
    }

    public void setLastProfileEditAt(Instant lastProfileEditAt) {
        this.lastProfileEditAt = lastProfileEditAt;
    }
}
