package com.cinema.testcinema.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String theme = "LIGHT";

    @Column(nullable = false)
    private String language = "ru";

    @Column(name = "last_email_edit_at")
    private Instant lastEmailEditAt;

    public Long getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Instant getLastEmailEditAt() {
        return lastEmailEditAt;
    }

    public void setLastEmailEditAt(Instant lastEmailEditAt) {
        this.lastEmailEditAt = lastEmailEditAt;
    }
}
