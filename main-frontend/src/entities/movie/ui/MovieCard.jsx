import React, { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Check, Play, Plus, X } from "lucide-react";
import { useAuth } from "@/features/auth";
import { useFavorites } from "@/features/favorites";
import { getMovieGenres, getMoviePoster, getMovieRating, getMovieYear } from "@/shared/lib/insight";
import {
  guestFavoritesChangedEvent,
  readGuestFavorites,
  toggleGuestFavorite,
} from "@/shared/utils";
import "@/entities/movie/ui/MovieCard.css";

export default function MovieCard({ movie, showProgress = false, onClick }) {
  const { user } = useAuth();
  const userId = user?.id ? Number(user.id) : null;
  const { data: rawData = [], add, remove } = useFavorites(userId);
  const [hovered, setHovered] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [guestFavIds, setGuestFavIds] = useState(() => readGuestFavorites());

  useEffect(() => {
    if (userId) {
      return undefined;
    }

    const syncGuestFavorites = (event) => {
      setGuestFavIds(event?.detail?.ids || readGuestFavorites());
    };

    window.addEventListener(guestFavoritesChangedEvent, syncGuestFavorites);
    return () => window.removeEventListener(guestFavoritesChangedEvent, syncGuestFavorites);
  }, [userId]);

  const favIds = useMemo(() => {
    if (!userId) {
      return guestFavIds;
    }

    return Array.isArray(rawData)
      ? rawData.map((item) => String(item?.movieId ?? item))
      : [];
  }, [guestFavIds, rawData, userId]);

  const movieId = String(movie?.id ?? movie?.imdbId ?? "");
  const isFav = movieId ? favIds.includes(movieId) : false;

  const handleFavClick = async (event) => {
    event.preventDefault();
    event.stopPropagation();

    if (!movieId || isLoading) {
      return;
    }

    if (!userId) {
      const result = toggleGuestFavorite(movieId);
      setGuestFavIds(result.ids);
      return;
    }

    try {
      setIsLoading(true);
      if (isFav) {
        await remove.mutateAsync(movieId);
      } else {
        await add.mutateAsync(movieId);
      }
    } catch (error) {
      console.error("Ошибка при изменении избранного:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const poster = getMoviePoster(movie);
  const progress = movie.progress || 0;
  const rating = getMovieRating(movie);
  const year = getMovieYear(movie);
  const genres = getMovieGenres(movie);

  let btnIcon = <Plus size={16} />;
  if (isFav) {
    btnIcon = hovered ? <X size={16} /> : <Check size={16} />;
  }

  const btnClasses = [
    "add-to-list-btn",
    isFav && "add-to-list-btn--active",
    isFav && hovered && "add-to-list-btn--remove",
    isLoading && "add-to-list-btn--loading",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <div className="movie-card" onMouseEnter={() => setHovered(true)} onMouseLeave={() => setHovered(false)}>
      <Link to={`/movie/${movieId}`} className="movie-card-link" onClick={onClick}>
        <div className="movie-poster">
          <img src={poster} alt={movie.title} loading="lazy" />
          {showProgress && progress > 0 ? (
            <div className="movie-progress">
              <div className="movie-progress-bar" style={{ width: `${progress}%` }} />
            </div>
          ) : null}
          <div className="movie-overlay">
            <button className="play-btn" type="button" tabIndex={-1}>
              <Play size={22} fill="currentColor" />
            </button>
          </div>
        </div>
        <div className="movie-info">
          <div className="movie-info-row">
            <h3 className="movie-title">{movie.title}</h3>
            {rating ? <span className="movie-rating">★ {rating.toFixed(1)}</span> : null}
          </div>
          <div className="movie-meta">
            <span className="movie-year">{year || "—"}</span>
            {genres[0] ? <span className="movie-genre">{genres[0]}</span> : null}
          </div>
        </div>
      </Link>

      <button
        className={btnClasses}
        onClick={handleFavClick}
        title={isFav ? "Убрать из избранного" : "Добавить в избранное"}
        disabled={isLoading}
        type="button"
      >
        {btnIcon}
      </button>
    </div>
  );
}
