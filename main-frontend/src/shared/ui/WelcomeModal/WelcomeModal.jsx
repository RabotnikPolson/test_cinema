import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMovies } from "@/features/movies";
import {
  getMovieGenres,
  getMovieId,
  getMoviePoster,
  getMovieSynopsis,
  getMovieTitle,
  getMovieYear,
} from "@/shared/lib/insight";
import "@/shared/ui/WelcomeModal/WelcomeModal.css";

const FALLBACK_POSTER =
  "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=2094&auto=format&fit=crop";

const FALLBACK_MOVIES = [
  {
    title: "Казахстанское кино",
    plot: "Откройте для себя локальные премьеры и сильные истории на главной витрине INSIGHT.",
    year: 2024,
    genre: "Драма",
    posterUrl: FALLBACK_POSTER,
  },
];

export default function WelcomeModal() {
  const [visible, setVisible] = useState(false);
  const [closing, setClosing] = useState(false);
  const navigate = useNavigate();
  const { data: movies = [] } = useMovies();

  const featured = useMemo(() => {
    const pool = movies.length ? movies : FALLBACK_MOVIES;
    return pool[Math.floor(Math.random() * pool.length)] || null;
  }, [movies]);

  const posterUrl = featured ? getMoviePoster(featured) : FALLBACK_POSTER;
  const movieTitle = featured ? getMovieTitle(featured) : "Лучшее кино";
  const movieDesc = featured ? getMovieSynopsis(featured) || "Смотрите лучшие фильмы..." : "Смотрите лучшие фильмы...";
  const movieYear = featured ? getMovieYear(featured) : "";
  const movieGenre = featured ? getMovieGenres(featured)[0] || featured.genre || "Кино" : "Кино";
  const movieId = getMovieId(featured);

  useEffect(() => {
    const lastShown = localStorage.getItem("welcomeModalLastShown");
    const now = Date.now();
    const twoHours = 2 * 60 * 60 * 1000;

    if (!lastShown || now - parseInt(lastShown, 10) > twoHours) {
      const timer = setTimeout(() => {
        setVisible(true);
        localStorage.setItem("welcomeModalLastShown", now.toString());
      }, 500);
      return () => clearTimeout(timer);
    }

    return undefined;
  }, []);

  const close = () => {
    setClosing(true);
    setTimeout(() => {
      setVisible(false);
      setClosing(false);
    }, 350);
  };

  const handleWatch = () => {
    close();

    if (movieId) {
      navigate(`/movie/${movieId}`);
      return;
    }

    navigate("/");
  };

  if (!visible) {
    return null;
  }

  return (
    <div className={`wm-backdrop ${closing ? "wm-closing" : ""}`} onMouseDown={close}>
      <div className="wm-modal" onMouseDown={(event) => event.stopPropagation()}>
        <button className="wm-close" onClick={close} aria-label="Закрыть" type="button">
          &#x2715;
        </button>

        <div className="wm-content">
          <div className="wm-brand">INSIGHT</div>
          <h2 className="wm-title">{movieTitle}</h2>
          <p className="wm-desc">{movieDesc}</p>
          <div className="wm-meta">
            {movieYear ? <span className="wm-meta-item">{movieYear}</span> : null}
            {movieYear ? <span className="wm-meta-dot" /> : null}
            <span className="wm-meta-item">{movieGenre}</span>
          </div>
          <div className="wm-actions">
            <button className="wm-btn-primary" onClick={handleWatch} type="button">
              Смотреть
            </button>
            <button className="wm-btn-ghost" onClick={close} type="button">
              Не сейчас
            </button>
          </div>
        </div>

        <div className="wm-poster">
          <img src={posterUrl} alt={movieTitle} className="wm-poster-img" />
          <div className="wm-poster-fade" />
        </div>
      </div>
    </div>
  );
}
