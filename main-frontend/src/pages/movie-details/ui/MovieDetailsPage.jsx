import React, { useEffect, useMemo } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/features/auth";
import { logClick } from "@/shared/api/metricsApi";
import {
  addFavorite,
  getFavoritesByUser,
  removeFavorite,
} from "@/features/favorites";
import { useMovie } from "@/features/movies";
import { useRecommendationsTab } from "@/features/recommendations";
import {
  formatRuntimeLabel,
  getMovieAgeLabel,
  getMovieBackdrop,
  getMovieGenres,
  getMoviePoster,
  getMovieRating,
} from "@/shared/lib/insight";
import {
  guestFavoritesChangedEvent,
  readGuestFavorites,
  toggleGuestFavorite,
} from "@/shared/utils";
import "@/pages/movie-details/ui/MovieDetails.css";

const normalizeId = (value) => {
  if (value === undefined || value === null) return null;
  return String(value);
};

function RecommendationSection({ title, subtitle, movieId, type }) {
  const { data, isLoading } = useRecommendationsTab(type, movieId, 8);
  const items = data?.recommendations || [];

  if (!items.length && !isLoading) {
    return null;
  }

  return (
    <section className="recommendation-section glass">
      <div className="section-header">
        <div>
          <h3>{title}</h3>
          <p>{subtitle}</p>
        </div>
      </div>

      {isLoading ? (
        <div className="section-empty">Загрузка...</div>
      ) : (
        <div className="recommendation-row no-scrollbar">
          {items.map((item) => (
            <Link
              key={item.movie_id}
              to={`/movie/${item.movie_id}`}
              className="recommendation-card"
              onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
            >
              <div className="recommendation-cover">
                <img src={item.poster_url || getMoviePoster(item)} alt={item.title} loading="lazy" />
              </div>
              <div className="recommendation-body">
                <strong>{item.title}</strong>
                <span>{item.year || item.release_year || "—"}</span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </section>
  );
}

export default function MovieDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const { data: movie, isLoading, isError, error } = useMovie(id);
  const { user } = useAuth();
  const currentUserId = user?.id ? Number(user.id) : null;
  const normalizedUserId = normalizeId(currentUserId);
  const [guestFavIds, setGuestFavIds] = React.useState(() => readGuestFavorites());

  useEffect(() => {
    if (normalizedUserId) {
      return undefined;
    }

    const syncGuestFavorites = (event) => {
      setGuestFavIds(event?.detail?.ids || readGuestFavorites());
    };

    window.addEventListener(guestFavoritesChangedEvent, syncGuestFavorites);
    return () => window.removeEventListener(guestFavoritesChangedEvent, syncGuestFavorites);
  }, [normalizedUserId]);

  const { data: remoteFavIds = [], isLoading: favsLoading } = useQuery({
    queryKey: ["favorites", normalizedUserId],
    queryFn: async () => {
      if (!normalizedUserId) {
        return [];
      }
      const data = await getFavoritesByUser(normalizedUserId);
      return Array.isArray(data)
        ? data.map((item) => normalizeId(item.movieId))
        : [];
    },
    enabled: !!normalizedUserId,
    staleTime: 30000,
  });

  const addMut = useMutation({
    mutationFn: (movieId) => addFavorite(normalizedUserId, movieId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["favorites", normalizedUserId] }),
  });

  const delMut = useMutation({
    mutationFn: (movieId) => removeFavorite(normalizedUserId, movieId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["favorites", normalizedUserId] }),
  });

  useEffect(() => {
    if (movie) {
      document.title = `${movie.title} — INSIGHT`;
      logClick(movie.id);
    }
  }, [movie, normalizedUserId]);

  const movieId = movie?.id ?? null;
  const normalizedMovieId = normalizeId(movieId);
  const favIds = normalizedUserId ? remoteFavIds : guestFavIds;
  const isFavorite = normalizedMovieId ? favIds.includes(normalizedMovieId) : false;

  const toggleFavorite = () => {
    if (!movieId || !normalizedMovieId) {
      return;
    }

    if (normalizedUserId) {
      if (isFavorite) {
        delMut.mutate(movieId);
      } else {
        addMut.mutate(movieId);
      }
      return;
    }

    const result = toggleGuestFavorite(normalizedMovieId);
    setGuestFavIds(result.ids);
  };

  const cast = useMemo(() => {
    const raw = movie?.raw?.actors || movie?.raw?.Actors || movie?.actors;
    if (!raw) {
      return [];
    }
    return raw
      .split(",")
      .map((item) => item.trim())
      .filter(Boolean)
      .slice(0, 10);
  }, [movie]);

  const creators = useMemo(() => {
    const director = movie?.raw?.director || movie?.raw?.Director;
    const writer = movie?.raw?.writer || movie?.raw?.Writer;
    return [director, writer]
      .filter(Boolean)
      .flatMap((value) => String(value).split(",").map((item) => item.trim()))
      .filter(Boolean);
  }, [movie]);

  if (isLoading) {
    return <div className="loading container">Загрузка фильма...</div>;
  }

  if (isError) {
    return (
      <div className="container">
        <div className="error">Ошибка: {error?.message || "Не удалось загрузить фильм"}</div>
        <button className="button button--ghost" onClick={() => navigate(-1)}>
          Назад
        </button>
      </div>
    );
  }

  if (!movie) {
    return (
      <div className="container">
        <div className="error">Фильм не найден</div>
        <Link to="/" className="button button--ghost">
          На главную
        </Link>
      </div>
    );
  }

  const genres = getMovieGenres(movie);
  const rating = getMovieRating(movie);
  const runtime = formatRuntimeLabel(movie);
  const ageLabel = getMovieAgeLabel(movie);

  return (
    <div className="details-page">
      <div
        className="details-hero"
        style={{
          backgroundImage: `linear-gradient(180deg, rgba(7,7,15,0.92) 0%, rgba(7,7,15,0.58) 35%, rgba(7,7,15,0.96) 100%), url(${getMovieBackdrop(movie)})`,
        }}
      >
        <div className="details-hero-grid">
          <div className="details-poster-panel glass">
            <img className="details-poster" src={getMoviePoster(movie)} alt={movie.title} loading="lazy" />
            <div className="details-poster-meta">
              {movie.year ? <span>{movie.year}</span> : null}
              {runtime ? <span>{runtime}</span> : null}
              {ageLabel ? <span>{ageLabel}</span> : null}
            </div>
          </div>

          <div className="details-hero-copy glass">
            <div className="details-tagline">
              {movie.tagline || movie.subtitle || "Смотреть в высоком качестве"}
            </div>
            <h1 className="details-title">{movie.title}</h1>
            <p className="details-subtitle">{movie.originalTitle || movie.title}</p>

            <div className="details-chips">
              {genres.slice(0, 4).map((genre) => (
                <span key={genre} className="details-chip">
                  {genre}
                </span>
              ))}
            </div>

            <div className="details-ratings">
              <div className="details-score">
                <strong>{rating ? rating.toFixed(1) : movie.imdbRating || "—"}</strong>
                <span>Рейтинг</span>
              </div>
              <div className="details-data">
                <div>
                  <strong>{movie.votes || movie.vote_count || "—"}</strong>
                  <span>голосов</span>
                </div>
                <div>
                  <strong>{movie.popularity || "—"}</strong>
                  <span>популярность</span>
                </div>
              </div>
            </div>

            <div className="details-actions">
              <button className="button btn-primary" onClick={() => navigate(`/movie/${id}/watch`)}>
                Смотреть
              </button>
              <button
                className={`button button--ghost ${isFavorite ? "favorite-active" : ""}`}
                onClick={toggleFavorite}
                disabled={addMut.isPending || delMut.isPending || favsLoading}
              >
                {isFavorite ? "В избранном" : "В избранное"}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="details-body container">
        <div className="details-summary glass">
          <h2>Описание</h2>
          <p>{movie.description || "Описание отсутствует."}</p>
        </div>

        <div className="details-crew glass">
          <div className="details-crew-block">
            <h3>Авторы</h3>
            <div className="details-crew-grid">
              {creators.map((name) => (
                <div key={name} className="details-crew-card">
                  <div className="details-crew-avatar">
                    {name.split(" ").map((part) => part[0]).join("")}
                  </div>
                  <div>
                    <div className="details-crew-name">{name}</div>
                    <div className="details-crew-role">Авторы</div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="details-crew-block">
            <h3>Актеры</h3>
            <div className="details-cast-grid">
              {cast.map((actor) => (
                <div key={actor} className="details-cast-card">
                  <div className="details-cast-avatar">
                    {actor.split(" ").map((part) => part[0]).join("")}
                  </div>
                  <div className="details-cast-name">{actor}</div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="details-sections">
          <RecommendationSection
            title="Серия фильмов"
            subtitle="Автоматические продолжения и предыдущие части"
            type="franchise"
            movieId={movie.id}
          />

          <RecommendationSection
            title="Похожие фильмы"
            subtitle="Фильмы с похожим визуальным стилем и настроением"
            type="content"
            movieId={movie.id}
          />

          <RecommendationSection
            title="Рекомендации для вас"
            subtitle="Подборка на основе ваших просмотров"
            type="hybrid"
            movieId={movie.id}
          />

          <RecommendationSection
            title="Другие истории жанра"
            subtitle="Дополнительный контент из этой категории"
            type="genre"
            movieId={movie.id}
          />
        </div>
      </div>
    </div>
  );
}
