package com.cinema.testcinema.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "movies")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Идентификаторы ────────────────────────────────────────────────────
    @Column(nullable = false)
    private String title;

    @Column(unique = true)
    private String imdbId;

    @Column(name = "kinopoisk_id")
    private String kinopoiskId;

    @Column(name = "kinopoisk_hd_id")
    private String kinopoiskHdId;

    // ── Названия ──────────────────────────────────────────────────────────
    @Column(name = "name_en", length = 500)
    private String nameEn;

    @Column(name = "name_original", length = 500)
    private String nameOriginal;

    // ── Медиа ─────────────────────────────────────────────────────────────
    private String posterUrl;

    @Column(name = "cover_url", length = 1000)
    private String coverUrl;

    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    // ── Описания ──────────────────────────────────────────────────────────
    @Column(length = 3000)
    private String description;

    @Column(name = "short_description", length = 1000)
    private String shortDescription;

    private String slogan;

    @Column(name = "editor_annotation", length = 1000)
    private String editorAnnotation;

    // ── Мета-информация ───────────────────────────────────────────────────
    private Long year;
    private String country;
    private String runtime;
    private String language;

    @Column(name = "content_type", length = 50)
    private String contentType;

    // ── Рейтинги ──────────────────────────────────────────────────────────
    @Column(name = "rating_kinopoisk", precision = 4, scale = 2)
    private BigDecimal ratingKinopoisk;

    @Column(name = "rating_kinopoisk_vote_count")
    private Integer ratingKinopoiskVoteCount;

    private String imdbRating;

    @Column(name = "rating_imdb_vote_count")
    private Integer ratingImdbVoteCount;

//    private String imdbVotes;
    private String rottenTomatoesRating;
    private String metacriticRating;

    // ── Возрастные рейтинги ───────────────────────────────────────────────
    @Column(name = "rating_mpaa", length = 20)
    private String ratingMpaa;

    @Column(name = "rating_age", length = 20)
    private String ratingAge;

    // ── Производство ──────────────────────────────────────────────────────
    private String director;

    @Column(length = 1000)
    private String actors;

    @Column(name = "production_status", length = 100)
    private String productionStatus;

    private String genreText;
    private String released;

    // ── Технические флаги ─────────────────────────────────────────────────
    @Column(name = "is_domestic", nullable = false)
    private boolean isDomestic = false;

    @Column(name = "kz_cultural_weight", nullable = false)
    private int kzCulturalWeight = 1;

    @Column(name = "is_serial", nullable = false)
    private boolean isSerial = false;

    @Column(name = "is_short_film", nullable = false)
    private boolean isShortFilm = false;

    @Column(name = "has_imax", nullable = false)
    private boolean hasImax = false;

    @Column(name = "has_3d", nullable = false)
    private boolean has3d = false;

    @Transient
    private String streamUrl;

    @Column(name = "tmdb_id")
    private Long tmdbId;
    // ── Жанры (many-to-many) ──────────────────────────────────────────────
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "movie_genres",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    public Movie() {}

    public Movie(String title, String imdbId, Long year) {
        this.title = title;
        this.imdbId = imdbId;
        this.year = year;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getImdbId() { return imdbId; }
    public void setImdbId(String imdbId) { this.imdbId = imdbId; }

    public String getKinopoiskId() { return kinopoiskId; }
    public void setKinopoiskId(String kinopoiskId) { this.kinopoiskId = kinopoiskId; }

    public String getKinopoiskHdId() { return kinopoiskHdId; }
    public void setKinopoiskHdId(String kinopoiskHdId) { this.kinopoiskHdId = kinopoiskHdId; }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }

    public String getNameOriginal() { return nameOriginal; }
    public void setNameOriginal(String nameOriginal) { this.nameOriginal = nameOriginal; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }

    public String getSlogan() { return slogan; }
    public void setSlogan(String slogan) { this.slogan = slogan; }

    public String getEditorAnnotation() { return editorAnnotation; }
    public void setEditorAnnotation(String editorAnnotation) { this.editorAnnotation = editorAnnotation; }

    public Long getYear() { return year; }
    public void setYear(Long year) { this.year = year; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getRuntime() { return runtime; }
    public void setRuntime(String runtime) { this.runtime = runtime; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public BigDecimal getRatingKinopoisk() { return ratingKinopoisk; }
    public void setRatingKinopoisk(BigDecimal ratingKinopoisk) { this.ratingKinopoisk = ratingKinopoisk; }

    public Integer getRatingKinopoiskVoteCount() { return ratingKinopoiskVoteCount; }
    public void setRatingKinopoiskVoteCount(Integer ratingKinopoiskVoteCount) { this.ratingKinopoiskVoteCount = ratingKinopoiskVoteCount; }

    public String getImdbRating() { return imdbRating; }
    public void setImdbRating(String imdbRating) { this.imdbRating = imdbRating; }

    public Integer getRatingImdbVoteCount() { return ratingImdbVoteCount; }
    public void setRatingImdbVoteCount(Integer ratingImdbVoteCount) { this.ratingImdbVoteCount = ratingImdbVoteCount; }

//    public String getImdbVotes() { return imdbVotes; }
//    public void setImdbVotes(String imdbVotes) { this.imdbVotes = imdbVotes; }

    public String getRottenTomatoesRating() { return rottenTomatoesRating; }
    public void setRottenTomatoesRating(String rottenTomatoesRating) { this.rottenTomatoesRating = rottenTomatoesRating; }

    public String getMetacriticRating() { return metacriticRating; }
    public void setMetacriticRating(String metacriticRating) { this.metacriticRating = metacriticRating; }

    public String getRatingMpaa() { return ratingMpaa; }
    public void setRatingMpaa(String ratingMpaa) { this.ratingMpaa = ratingMpaa; }

    public String getRatingAge() { return ratingAge; }
    public void setRatingAge(String ratingAge) { this.ratingAge = ratingAge; }

    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }

    public String getActors() { return actors; }
    public void setActors(String actors) { this.actors = actors; }

    public String getProductionStatus() { return productionStatus; }
    public void setProductionStatus(String productionStatus) { this.productionStatus = productionStatus; }

    public String getGenreText() { return genreText; }
    public void setGenreText(String genreText) { this.genreText = genreText; }

    public String getReleased() { return released; }
    public void setReleased(String released) { this.released = released; }

    public boolean isDomestic() { return isDomestic; }
    public void setDomestic(boolean domestic) { isDomestic = domestic; }

    public int getKzCulturalWeight() { return kzCulturalWeight; }
    public void setKzCulturalWeight(int kzCulturalWeight) { this.kzCulturalWeight = kzCulturalWeight; }

    public boolean isSerial() { return isSerial; }
    public void setSerial(boolean serial) { isSerial = serial; }

    public boolean isShortFilm() { return isShortFilm; }
    public void setShortFilm(boolean shortFilm) { isShortFilm = shortFilm; }

    public boolean isHasImax() { return hasImax; }
    public void setHasImax(boolean hasImax) { this.hasImax = hasImax; }

    public boolean isHas3d() { return has3d; }
    public void setHas3d(boolean has3d) { this.has3d = has3d; }

    public String getStreamUrl() { return streamUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }

    public Set<Genre> getGenres() { return genres; }
    public void setGenres(Set<Genre> genres) { this.genres = genres; }

    public Long getTmdbId() { return tmdbId; }
    public void setTmdbId(Long tmdbId) { this.tmdbId = tmdbId; }
}
