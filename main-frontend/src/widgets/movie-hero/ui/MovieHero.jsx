import React from "react";
import { Info, Play } from "lucide-react";
import { useNavigate } from "react-router-dom";
import {
  formatRuntimeLabel,
  getMovieBackdrop,
  getMovieGenres,
  getMovieId,
  getMovieRating,
  getMovieSynopsis,
  getMovieTitle,
  getMovieYear,
} from "@/shared/lib/insight";
import "@/widgets/movie-hero/ui/MovieHero.css";

export default function HeroBanner({ movie }) {
  const navigate = useNavigate();

  if (!movie) {
    return null;
  }

  const movieId = getMovieId(movie);
  const title = getMovieTitle(movie);
  const year = getMovieYear(movie);
  const ratingValue = getMovieRating(movie);
  const runtimeLabel = formatRuntimeLabel(movie);
  const genres = getMovieGenres(movie);
  const filledStars = Math.max(0, Math.min(5, Math.floor((ratingValue || 0) / 2)));

  const openMovie = () => {
    if (movieId) {
      navigate(`/movie/${movieId}`);
    }
  };

  const openWatch = () => {
    if (movieId) {
      navigate(`/movie/${movieId}/watch`);
    }
  };

  return (
    <header className="hero-banner">
      <img
        src={getMovieBackdrop(movie)}
        alt={title}
        className="hero-banner-image"
      />

      <div className="hero-banner-overlay" />
      <div className="hero-banner-gradient-left" />
      <div className="hero-banner-gradient-top" />

      <div className="hero-banner-content">
        <div className="hero-banner-label">Эксклюзивная премьера</div>

        <h1 className="hero-banner-title">{title}</h1>

        <p className="hero-banner-description">
          {getMovieSynopsis(movie) || "Откройте для себя новые истории, собранные для тихого кинозала дома."}
        </p>

        <div className="hero-banner-rating">
          {ratingValue ? (
            <>
              <span className="rating-stars">
                {"★".repeat(filledStars)}
                {"☆".repeat(5 - filledStars)}
              </span>
              <span className="rating-value">{ratingValue.toFixed(1)} / 10</span>
            </>
          ) : null}
          {year ? <span className="rating-year">{year}</span> : null}
          {runtimeLabel ? <span className="rating-chip">{runtimeLabel}</span> : null}
          {genres[0] ? <span className="rating-chip">{genres[0]}</span> : null}
        </div>

        <div className="hero-banner-actions">
          <button className="hero-btn hero-btn-primary" onClick={openWatch} type="button">
            <Play size={18} />
            Смотреть сейчас
          </button>

          <button className="hero-btn hero-btn-secondary" onClick={openMovie} type="button">
            <Info size={18} />
            Подробнее
          </button>
        </div>
      </div>
    </header>
  );
}
