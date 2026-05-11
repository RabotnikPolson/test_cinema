package com.cinema.testcinema.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "search_logs")
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", updatable = false)
    private Long userId;

    @Column(name = "guest_session_id", updatable = false)
    private String guestSessionId;

    @Column(nullable = false, length = 255, updatable = false)
    private String query;

    @Column(name = "result_count", nullable = false, updatable = false)
    private Integer resultCount = 0;

    @Column(name = "searched_at", nullable = false, updatable = false)
    private Instant searchedAt = Instant.now();

    public SearchLog() {}

    public SearchLog(Long userId, String guestSessionId, String query, Integer resultCount) {
        this.userId = userId;
        this.guestSessionId = guestSessionId;
        this.query = query;
        this.resultCount = resultCount;
        this.searchedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getGuestSessionId() { return guestSessionId; }
    public String getQuery() { return query; }
    public Integer getResultCount() { return resultCount; }
    public Instant getSearchedAt() { return searchedAt; }
}
