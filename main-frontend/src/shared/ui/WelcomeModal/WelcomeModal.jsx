import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMovies } from "@/features/movies";
import "@/shared/ui/WelcomeModal/WelcomeModal.css";

const FALLBACK_POSTER =
  "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=2094&auto=format&fit=crop";


const KAZAKH_MOVIES = [
  {
    id: "peish",
    title: "Пейіш: Жұмақ ананың табанының астында",
    plot: "Пронзительная история о материнской любви, самопожертвовании и пути к прощению. Фильм, заставляющий задуматься о самых главных ценностях.",
    year: 2024,
    genre: "ДРАМА",
    posterUrl: "https://images.unsplash.com/photo-1485846234645-a62644f84728?q=80&w=2000&auto=format&fit=crop"
  },
  {
    id: "vzaperti",
    title: "Взаперти",
    plot: "Квартира становится ловушкой. Психологический триллер, где каждый шаг может стать последним, а напряжение нарастает с каждой минутой.",
    year: 2023,
    genre: "ТРИЛЛЕР, ДРАМА",
    posterUrl: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=2000&auto=format&fit=crop"
  },
  {
    id: "dastur",
    title: "Дастур",
    plot: "Девушка сталкивается с жестокими традициями и патриархальными устоями в ауле. Казахстанский хоррор, поднимающий острые социальные темы.",
    year: 2023,
    genre: "ХОРРОР, ДРАМА",
    posterUrl: "https://images.unsplash.com/photo-1509248961158-e54f6934749c?q=80&w=2000&auto=format&fit=crop"
  }
];

export default function WelcomeModal() {
  const [visible, setVisible] = useState(false);
  const [closing, setClosing] = useState(false);
  const [featured, setFeatured] = useState(null);
  const navigate = useNavigate();
  const { data: movies = [] } = useMovies();

  useEffect(() => {
    // Pick a random movie from the 3 variations only once on mount
    const randomMovie = KAZAKH_MOVIES[Math.floor(Math.random() * KAZAKH_MOVIES.length)];
    setFeatured(randomMovie);
  }, []);

  const posterUrl = featured?.posterUrl || FALLBACK_POSTER;
  const movieTitle = featured?.title || "Лучшее кино";
  const movieDesc = featured?.plot || "Смотрите лучшие фильмы...";
  const movieYear = featured?.year || "";
  const movieGenre = featured?.genre || "Кино";

  useEffect(() => {
    const timer = setTimeout(() => setVisible(true), 500);
    return () => clearTimeout(timer);
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
    if (featured?.id || featured?.imdbId) {
      navigate(`/movie/${featured.id || featured.imdbId}`);
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
        <button className="wm-close" onClick={close} aria-label="Закрыть">
          &#x2715;
        </button>

        <div className="wm-content">
          <div className="wm-brand">INSIGHT</div>
          <h2 className="wm-title">{movieTitle}</h2>
          <p className="wm-desc">{movieDesc}</p>
          <div className="wm-meta">
            {movieYear && <span className="wm-meta-item">{movieYear}</span>}
            {movieYear && <span className="wm-meta-dot" />}
            <span className="wm-meta-item">{movieGenre}</span>
          </div>
          <div className="wm-actions">
            <button className="wm-btn-primary" onClick={handleWatch}>
              Смотреть
            </button>
            <button className="wm-btn-ghost" onClick={close}>
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
