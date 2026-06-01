import React, { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Heart, Play } from "lucide-react";
import { MovieCard } from "@/entities/movie";
import { useAuth } from "@/features/auth";
import { useFavorites } from "@/features/favorites";
import { useMovie, useMovies } from "@/features/movies";
import { useRecommendationsTab } from "@/features/recommendations";
import { logClick } from "@/shared/api/metricsApi";
import {
  formatRuntimeLabel,
  getInitials,
  getMovieAgeLabel,
  getMovieBackdrop,
  getMovieGenres,
  getMovieId,
  getMoviePoster,
  getMovieRating,
  getMovieSynopsis,
  getMovieTitle,
  getMovieYear,
} from "@/shared/lib/insight";
import { MovieCarousel } from "@/shared/ui";
import {
  guestFavoritesChangedEvent,
  readGuestFavorites,
  toggleGuestFavorite,
} from "@/shared/utils";
import "@/pages/movie-details/ui/MovieDetails.css";

const COUNTRY_FLAGS = {
  'казахстан': '🇰🇿', 'россия': '🇷🇺', 'сша': '🇺🇸', 'китай': '🇨🇳',
  'франция': '🇫🇷', 'великобритания': '🇬🇧', 'германия': '🇩🇪', 'япония': '🇯🇵',
  'южная корея': '🇰🇷', 'италия': '🇮🇹', 'испания': '🇪🇸', 'австралия': '🇦🇺',
  'канада': '🇨🇦', 'индия': '🇮🇳', 'израиль': '🇮🇱', 'швеция': '🇸🇪',
  'дания': '🇩🇰', 'норвегия': '🇳🇴', 'финляндия': '🇫🇮', 'польша': '🇵🇱',
  'австрия': '🇦🇹', 'нидерланды': '🇳🇱', 'бельгия': '🇧🇪', 'ирландия': '🇮🇪',
  'португалия': '🇵🇹', 'греция': '🇬🇷', 'турция': '🇹🇷', 'иран': '🇮🇷',
  'мексика': '🇲🇽', 'бразилия': '🇧🇷', 'аргентина': '🇦🇷', 'гонконг': '🇭🇰',
  'тайвань': '🇹🇼', 'таиланд': '🇹🇭', 'чехия': '🇨🇿', 'венгрия': '🇭🇺',
  'румыния': '🇷🇴', 'сербия': '🇷🇸', 'украина': '🇺🇦', 'беларусь': '🇧🇾',
  'грузия': '🇬🇪', 'узбекистан': '🇺🇿', 'азербайджан': '🇦🇿', 'армения': '🇦🇲',
  'ссср': '🇷🇺', 'ссс': '🇷🇺',
};

const KNOWN_CONTENT_TYPES = new Set([
  "FILM", "SERIAL", "MINI_SERIES", "TV_SHOW", "VIDEO", "SHORT_FILM",
]);

const RAIL_BREAKPOINTS = {
  320: { slidesPerView: 1.2, spaceBetween: 12 },
  640: { slidesPerView: 2.2, spaceBetween: 14 },
  900: { slidesPerView: 3.2, spaceBetween: 16 },
  1200: { slidesPerView: 4.2, spaceBetween: 18 },
  1440: { slidesPerView: 5, spaceBetween: 18 },
};

const normalizeId = (value) => {
  if (value === undefined || value === null) {
    return null;
  }

  return String(value);
};

function RecommendationSection({ title, subtitle, movieId, type, movieLookup }) {
  const { t } = useTranslation();
  const { data, isLoading } = useRecommendationsTab(type, movieId, 10);
  const rawItems = data?.recommendations || [];

  const items = useMemo(() => {
    if (!movieLookup?.size) return rawItems;
    return rawItems
      .map((item) => {
        const key = String(item.movie_id ?? item.id ?? "");
        const catalogMovie = movieLookup.get(key);
        return catalogMovie ? { ...item, ...catalogMovie } : item;
      })
      .filter((item) => getMovieId(item) != null);
  }, [rawItems, movieLookup]);

  if (!items.length && !isLoading) {
    return null;
  }

  return (
    <section className="recommendation-section glass">
      <div className="section-header recommendation-header">
        <div>
          <h3>{title}</h3>
          <p>{subtitle}</p>
        </div>
      </div>

      {isLoading ? (
        <div className="section-empty">{t("common.loading")}</div>
      ) : (
        <MovieCarousel
          items={items}
          renderSlide={(item) => (
            <MovieCard
              movie={item}
              showFavorite={false}
              onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
            />
          )}
          keyExtractor={(item, i) => `${type}-${getMovieId(item) ?? i}`}
          breakpoints={RAIL_BREAKPOINTS}
        />
      )}
    </section>
  );
}

export default function MovieDetailsPage() {
  const { t } = useTranslation();
  const { id } = useParams();
  const navigate = useNavigate();
  const { data: movie, isLoading, isError, error } = useMovie(id);
  const { data: allMovies = [] } = useMovies();
  const { user } = useAuth();
  const currentUserId = user?.id ? Number(user.id) : null;
  const normalizedUserId = normalizeId(currentUserId);
  const [guestFavIds, setGuestFavIds] = useState(() => readGuestFavorites());

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

  const { data: rawData = [], isLoading: favsLoading, add, remove } = useFavorites(normalizedUserId);

  const favIds = useMemo(() => {
    if (normalizedUserId) {
      return Array.isArray(rawData) ? rawData.map((item) => normalizeId(item?.movieId ?? item)) : [];
    }

    return guestFavIds;
  }, [guestFavIds, normalizedUserId, rawData]);

  useEffect(() => {
    if (movie) {
      document.title = `${getMovieTitle(movie)} — INSIGHT`;
      logClick(normalizedUserId, getMovieId(movie), "browse");
    }
  }, [movie, normalizedUserId]);

  const movieId = getMovieId(movie);
  const normalizedMovieId = normalizeId(movieId);
  const isFavorite = normalizedMovieId ? favIds.includes(normalizedMovieId) : false;

  const toggleFavorite = () => {
    if (!movieId || !normalizedMovieId) {
      return;
    }

    if (normalizedUserId) {
      if (isFavorite) {
        remove.mutate(movieId);
      } else {
        add.mutate(movieId);
      }
      return;
    }

    const result = toggleGuestFavorite(normalizedMovieId);
    setGuestFavIds(result.ids);
  };

  const movieLookup = useMemo(() => {
    const map = new Map();
    allMovies.forEach((m) => {
      if (m.id != null) map.set(String(m.id), m);
      if (m.imdbId) map.set(String(m.imdbId), m);
    });
    return map;
  }, [allMovies]);

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
    return <div className="loading container">{t("details.loadingMovie")}</div>;
  }

  if (isError) {
    return (
      <div className="container">
        <div className="error">{t("common.error")}: {error?.message || t("details.loadError")}</div>
        <button className="button button--ghost" onClick={() => navigate(-1)} type="button">
          {t("common.back")}
        </button>
      </div>
    );
  }

  if (!movie) {
    return (
      <div className="container">
        <div className="error">{t("details.notFound")}</div>
        <Link to="/" className="button button--ghost">
          {t("common.toHome")}
        </Link>
      </div>
    );
  }

  const genres = getMovieGenres(movie);
  const rating = getMovieRating(movie);
  const runtime = formatRuntimeLabel(movie);
  const ageLabel = getMovieAgeLabel(movie);
  const title = getMovieTitle(movie);
  const year = getMovieYear(movie);
  const synopsis = getMovieSynopsis(movie) || t("details.noSynopsis");

  return (
    <div className="details-page">
      <div
        className="details-hero"
        style={{
          backgroundImage: `linear-gradient(180deg, rgba(17, 17, 17, 0.92) 0%, rgba(17, 17, 17, 0.58) 35%, rgba(17, 17, 17, 0.96) 100%), url(${getMovieBackdrop(movie)})`,
        }}
      >
        <div className="details-hero-grid">
          <div className="details-poster-panel glass">
            <img className="details-poster" src={getMoviePoster(movie)} alt={title} loading="lazy" />
            <div className="details-poster-meta">
              {year ? <span>{year}</span> : null}
              {runtime ? <span>{runtime}</span> : null}
              {ageLabel ? <span>{ageLabel}</span> : null}
            </div>
          </div>

          <div className="details-hero-copy glass">
            <div className="details-tagline">{movie.slogan || movie.tagline || t("details.watchHq")}</div>
            <h1 className="details-title">{title}</h1>
            {(movie.nameOriginal || movie.nameEn) && (
              <p className="details-subtitle">{movie.nameOriginal || movie.nameEn}</p>
            )}
            {movie.shortDescription && (
              <p className="details-short-desc">{movie.shortDescription}</p>
            )}

            <div className="details-chips">
              {genres.slice(0, 4).map((genre) => (
                <span key={genre} className="details-chip">
                  {genre}
                </span>
              ))}
            </div>

            <div className="details-meta">
              {movie.country && movie.country.split(",").map(c => c.trim()).filter(Boolean).map(c => (
                <span key={c} className="details-meta-chip details-meta-chip--country">
                  {c}
                </span>
              ))}
              {movie.ratingAge && (
                <span className="details-meta-chip details-meta-chip--age">{movie.ratingAge}</span>
              )}
              {movie.contentType && KNOWN_CONTENT_TYPES.has(movie.contentType) && (
                <span className="details-meta-chip details-meta-chip--type">
                  {t(`details.contentType.${movie.contentType}`)}
                </span>
              )}
            </div>

            {(movie.ratingKinopoisk || movie.imdbRating) && (
              <div className="details-rating-row">
                {movie.ratingKinopoisk && (
                  <div className="details-rating-block details-rating-block--kp">
                    <span className="details-rating-source">Кинопоиск</span>
                    <span className="details-rating-number">{Number(movie.ratingKinopoisk).toFixed(1)}</span>
                  </div>
                )}
                {movie.imdbRating && (
                  <div className="details-rating-block details-rating-block--imdb">
                    <span className="details-rating-source">IMDb</span>
                    <span className="details-rating-number">{movie.imdbRating}</span>
                  </div>
                )}
              </div>
            )}

            <div className="details-actions">
              <button className="details-btn-watch" onClick={() => navigate(`/movie/${id}/watch`)} type="button">
                <Play size={18} fill="currentColor" />
                {t("hero.watch")}
              </button>
              <button
                className={`details-btn-fav ${isFavorite ? "is-fav" : ""}`}
                onClick={toggleFavorite}
                disabled={add.isPending || remove.isPending || favsLoading}
                type="button"
              >
                <Heart size={16} fill={isFavorite ? "currentColor" : "none"} />
                {isFavorite ? t("details.inFavorites") : t("details.addToFavorites")}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="details-body">
        <section className="details-summary glass">
          <h2>{t("details.description")}</h2>
          <p>{synopsis}</p>
        </section>

        <section className="details-crew glass">
          <div className="details-crew-block">
            <h3>{t("details.authors")}</h3>
            <div className="details-crew-grid">
              {creators.map((name) => (
                <div key={name} className="details-crew-card">
                  <div className="details-crew-avatar">{getInitials(name)}</div>
                  <div className="details-person-meta">
                    <div className="details-crew-name">{name}</div>
                    <div className="details-crew-role">{t("details.author")}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="details-crew-block">
            <h3>{t("details.cast")}</h3>
            <div className="details-cast-grid">
              {cast.map((actor) => (
                <div key={actor} className="details-cast-card">
                  <div className="details-cast-avatar">{getInitials(actor)}</div>
                  <div className="details-person-meta">
                    <div className="details-cast-name">{actor}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>

        <div className="details-sections">
          <RecommendationSection
            title={t("details.rec.franchiseTitle")}
            subtitle={t("details.rec.franchiseSub")}
            type="franchise"
            movieId={movieId}
            movieLookup={movieLookup}
          />

          <RecommendationSection
            title={t("details.rec.similarTitle")}
            subtitle={t("details.rec.similarSub")}
            type="content"
            movieId={movieId}
            movieLookup={movieLookup}
          />

          <RecommendationSection
            title={t("details.rec.forYouTitle")}
            subtitle={t("details.rec.forYouSub")}
            type="hybrid"
            movieId={movieId}
            movieLookup={movieLookup}
          />

          <RecommendationSection
            title={t("details.rec.genreTitle")}
            subtitle={t("details.rec.genreSub")}
            type="genre"
            movieId={movieId}
            movieLookup={movieLookup}
          />
        </div>
      </div>
    </div>
  );
}
