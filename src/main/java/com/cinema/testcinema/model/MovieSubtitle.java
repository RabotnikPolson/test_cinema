package com.cinema.testcinema.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "movie_subtitles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieSubtitle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(name = "original_language", nullable = false, length = 10)
    private String originalLanguage;

    @Column(name = "os_file_id", nullable = false, unique = true, length = 50)
    private String osFileId;

    @Column(name = "os_subtitle_id", length = 50)
    private String osSubtitleId;

    @Column(nullable = false, length = 10)
    private String format;

    @Column(name = "storage_path", length = 500)
    private String storagePath;

    @Column(name = "is_downloaded", nullable = false)
    @Builder.Default
    private boolean isDownloaded = false;

    @Column(name = "needs_translation", nullable = false)
    @Builder.Default
    private boolean needsTranslation = false;

    @Column(name = "target_language", length = 10)
    private String targetLanguage;

    @Column(name = "is_vectorized_for_ai", nullable = false)
    @Builder.Default
    private boolean isVectorizedForAi = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
