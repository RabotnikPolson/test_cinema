import React from "react";
import { Link } from "react-router-dom";
import { Play, Plus } from "lucide-react";
import "@/shared/styles/components/MovieCard.css";

export default function MovieCard({ movie, showProgress = false }) {
  const poster =
    movie.poster ||
    movie.posterUrl ||
    `https://placehold.jp/333/fff/300x450.png?text=${encodeURIComponent(
      movie.title
    )}`;

  const progress = movie.progress || 0;

  return (
    <div className="movie-card">
      <Link to={`/movie/${movie.id}`} className="movie-card-link">
        <div className="movie-poster">
          <img
            src={poster}
            alt={movie.title}
            loading="lazy"
            onError={(e) => {
              e.currentTarget.src =
                "https://placehold.jp/333/fff/300x450.png?text=No+Image";
            }}
          />
          {showProgress && progress > 0 && (
            <div className="movie-progress">
              <div
                className="movie-progress-bar"
                style={{ width: `${progress}%` }}
              />
            </div>
          )}
          <div className="movie-overlay">
            <button className="play-btn">
              <Play size={24} fill="currentColor" />
            </button>
          </div>
        </div>
        <div className="movie-info">
          <h3 className="movie-title">{movie.title}</h3>
          <div className="movie-meta">
            <span className="movie-year">{movie.year ?? movie.releaseDate?.split('-')[0] ?? "—"}</span>
            <span className="movie-rating">★ {movie.rating?.toFixed(1) ?? "—"}</span>
          </div>
          <div className="movie-genres">
            {movie.genre && (
              <span className="movie-genre">
                {typeof movie.genre === 'string' ? movie.genre.split(',')[0] : movie.genre}
              </span>
            )}
          </div>
        </div>
      </Link>
      <button className="add-to-list-btn">
        <Plus size={16} />
      </button>
    </div>
  );
}