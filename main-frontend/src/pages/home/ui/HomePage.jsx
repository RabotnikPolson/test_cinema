import React, { useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { ArrowRight, ChevronLeft, ChevronRight } from "lucide-react";
import { MovieCard } from "@/entities/movie";
import { useAuth } from "@/features/auth";
import { useMovies } from "@/features/movies";
import {
  useBecauseYouLiked,
  useKazakhstanMovies,
  useTrending,
} from "@/features/recommendations";
import { MovieCarousel, MovieGrid } from "@/shared/ui";
import {
  formatRuntimeLabel,
  getMovieGenres,
  getMovieId,
  getMoviePoster,
  getMovieRating,
  getMovieSynopsis,
  getMovieTitle,
  getMovieYear,
} from "@/shared/lib/insight";
import HeroBanner from "@/widgets/movie-hero/ui/MovieHero";
import "@/pages/home/ui/Home.css";

const RAIL_BREAKPOINTS = {
  320: { slidesPerView: 1.1, spaceBetween: 12 },
  560: { slidesPerView: 2.1, spaceBetween: 14 },
  900: { slidesPerView: 3.2, spaceBetween: 16 },
  1200: { slidesPerView: 4.2, spaceBetween: 18 },
  1440: { slidesPerView: 5, spaceBetween: 18 },
};

function buildMovieLookup(movies) {
  const lookup = new Map();

  movies.forEach((movie) => {
    if (movie?.id != null) {
      lookup.set(String(movie.id), movie);
    }

    if (movie?.imdbId) {
      lookup.set(String(movie.imdbId), movie);
    }
  });

  return lookup;
}

function enrichMovies(items, lookup) {
  return (items || [])
    .map((item) => {
      const movieId = getMovieId(item);
      const catalogMovie = movieId != null ? lookup.get(String(movieId)) : null;
      return catalogMovie ? { ...item, ...catalogMovie, movie_id: item.movie_id ?? catalogMovie.id } : item;
    })
    .filter((item) => getMovieId(item) != null);
}

function dedupeMovies(items) {
  const seen = new Set();

  return items.filter((movie) => {
    const movieId = getMovieId(movie);
    if (movieId == null) {
      return false;
    }

    const key = String(movieId);
    if (seen.has(key)) {
      return false;
    }

    seen.add(key);
    return true;
  });
}

function getPopularityScore(movie) {
  const rating = getMovieRating(movie) || 0;
  const recommendationScore = Number(movie?.score) || 0;
  const popularity = Number(movie?.popularity ?? movie?.raw?.popularity ?? movie?.raw?.voteCount ?? 0) || 0;

  return recommendationScore * 10 + popularity + rating * 5;
}

function buildExclusionSet(groups) {
  return new Set(
    groups
      .flat()
      .map((movie) => getMovieId(movie))
      .filter((movieId) => movieId != null)
      .map((movieId) => String(movieId)),
  );
}

function filterExcluded(items, excluded) {
  return items.filter((movie) => !excluded.has(String(getMovieId(movie))));
}

function getSpotlightOffset(index, activeIndex, total) {
  let offset = index - activeIndex;
  const half = Math.floor(total / 2);

  if (offset > half) {
    offset -= total;
  }

  if (offset < -half) {
    offset += total;
  }

  return offset;
}

function MovieRail({ title, items, linkTo, linkLabel }) {
  if (!items.length) {
    return null;
  }

  return (
    <section className="home-section">
      <div className="container">
        <div className="section-header">
          <h2 className="section-title">{title}</h2>
          {linkTo && linkLabel ? (
            <Link to={linkTo} className="section-link">
              {linkLabel}
              <ArrowRight size={16} />
            </Link>
          ) : null}
        </div>
        <MovieCarousel
          items={items}
          renderSlide={(movie) => <MovieCard movie={movie} showFavorite={false} />}
          keyExtractor={(movie, i) => `${title}-${getMovieId(movie) ?? i}`}
          breakpoints={RAIL_BREAKPOINTS}
        />
      </div>
    </section>
  );
}

export default function HomePage() {
  const { user } = useAuth();
  const userId = user?.profile?.userId || user?.id || null;
  const { data: movies = [], isLoading, isError, error } = useMovies();
  const { data: becauseYouLikedData, isError: isBecauseYouLikedError } = useBecauseYouLiked(userId);
  const { data: trendingData } = useTrending();
  const { data: kazakhData, isLoading: isKazakhLoading } = useKazakhstanMovies({
    userId,
    limit: 7,
    sortBy: "rating",
  });
  const [searchParams] = useSearchParams();
  const [activeKazakhIndex, setActiveKazakhIndex] = useState(0);
  const q = (searchParams.get("q") || "").toLowerCase();

  const movieLookup = useMemo(() => buildMovieLookup(movies), [movies]);

  const continueWatching = useMemo(
    () => movies.filter((movie) => movie.progress && movie.progress > 0).slice(0, 6),
    [movies],
  );

  const newReleases = useMemo(() => {
    return movies
      .filter((movie) => {
        const releaseDate = movie?.raw?.releaseDate || movie?.releaseDate;
        if (!releaseDate) {
          return false;
        }

        const release = new Date(releaseDate);
        const monthAgo = new Date();
        monthAgo.setMonth(monthAgo.getMonth() - 2);
        return release > monthAgo;
      })
      .slice(0, 18);
  }, [movies]);

  const filtered = useMemo(() => {
    return movies.filter((movie) => {
      if (!q) {
        return true;
      }

      const genresText = getMovieGenres(movie).join(" ").toLowerCase();
      return getMovieTitle(movie).toLowerCase().includes(q) || genresText.includes(q);
    });
  }, [movies, q]);

  const popularFallback = useMemo(() => {
    return dedupeMovies(
      [...movies].sort((a, b) => getPopularityScore(b) - getPopularityScore(a)),
    ).slice(0, 12);
  }, [movies]);

  const popularItems = useMemo(() => {
    const enrichedTrending = dedupeMovies(enrichMovies(trendingData?.recommendations || [], movieLookup));
    return (enrichedTrending.length ? enrichedTrending : popularFallback).slice(0, 12);
  }, [movieLookup, popularFallback, trendingData]);

  const personalizedItems = useMemo(() => {
    return dedupeMovies(enrichMovies(becauseYouLikedData?.recommendations || [], movieLookup)).slice(0, 12);
  }, [becauseYouLikedData, movieLookup]);

  const kazakhSpotlightItems = useMemo(() => {
    return dedupeMovies(enrichMovies(kazakhData?.recommendations || [], movieLookup)).slice(0, 7);
  }, [kazakhData, movieLookup]);

  const curatedIds = useMemo(
    () => buildExclusionSet([popularItems, personalizedItems, continueWatching, kazakhSpotlightItems]),
    [continueWatching, kazakhSpotlightItems, personalizedItems, popularItems],
  );

  const freshItems = useMemo(() => {
    const source = newReleases.length ? newReleases : movies;
    return dedupeMovies(filterExcluded(source, curatedIds)).slice(0, 12);
  }, [curatedIds, movies, newReleases]);

  const recentlyAddedItems = useMemo(() => {
    return [...movies]
      .filter((movie) => movie.createdAt)
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      .slice(0, 15);
  }, [movies]);

  const topRatedItems = useMemo(() => {
    const excluded = buildExclusionSet([
      popularItems,
      personalizedItems,
      continueWatching,
      kazakhSpotlightItems,
      freshItems,
    ]);

    return dedupeMovies(
      filterExcluded(
        [...movies].sort((a, b) => (getMovieRating(b) || 0) - (getMovieRating(a) || 0)),
        excluded,
      ),
    ).slice(0, 15);
  }, [continueWatching, freshItems, kazakhSpotlightItems, movies, personalizedItems, popularItems]);

  const safeKazakhIndex = kazakhSpotlightItems.length
    ? ((activeKazakhIndex % kazakhSpotlightItems.length) + kazakhSpotlightItems.length) % kazakhSpotlightItems.length
    : 0;
  const activeKazakhMovie = kazakhSpotlightItems[safeKazakhIndex];

  const stepKazakhCarousel = (direction) => {
    if (!kazakhSpotlightItems.length) {
      return;
    }

    setActiveKazakhIndex((current) => {
      return (current + direction + kazakhSpotlightItems.length) % kazakhSpotlightItems.length;
    });
  };

  return (
    <div className="home-page">
      <HeroBanner />

      <MovieRail
        title="Популярное сейчас"
        items={popularItems}
        linkTo="/trending"
        linkLabel="Вся лента"
      />

      <section className="home-section home-section--spotlight">
        <div className="container">
          <div className="section-header">
            <h2 className="section-title">Популярные казахстанские кино</h2>
          </div>

          {isKazakhLoading && !activeKazakhMovie ? (
            <div className="section-state">Загрузка подборки...</div>
          ) : null}

          {!isKazakhLoading && activeKazakhMovie ? (
            <div className="kaz-spotlight">
              <div className="kaz-spotlight-stage-wrap">
                <button
                  type="button"
                  className="kaz-spotlight-nav kaz-spotlight-nav--prev"
                  onClick={() => stepKazakhCarousel(-1)}
                  aria-label="Предыдущий фильм"
                >
                  <ChevronLeft size={18} />
                </button>

                <div className="kaz-spotlight-stage">
                  {kazakhSpotlightItems.map((movie, index) => {
                    const offset = getSpotlightOffset(index, safeKazakhIndex, kazakhSpotlightItems.length);
                    const absOffset = Math.abs(offset);
                    const movieId = getMovieId(movie);

                    if (absOffset > 3) {
                      return null;
                    }

                    return (
                      <Link
                        key={`kazakh-spotlight-${movieId}`}
                        to={`/movie/${movieId}`}
                        className={`kaz-spotlight-card${offset === 0 ? " is-active" : ""}`}
                        style={{
                          zIndex: kazakhSpotlightItems.length - absOffset,
                          opacity: 1 - absOffset * 0.22,
                          transform: `translate(-50%, -50%) translateX(calc(${offset} * clamp(88px, 12vw, 170px))) translateY(${absOffset * 18}px) scale(${1 - absOffset * 0.12}) rotateY(${offset * -18}deg)`,
                        }}
                        aria-label={`Открыть ${getMovieTitle(movie)}`}
                      >
                        <img
                          src={getMoviePoster(movie)}
                          alt={getMovieTitle(movie)}
                          className="kaz-spotlight-image"
                          loading="lazy"
                        />
                        <span className="kaz-spotlight-card-shadow" />
                        <span className="kaz-spotlight-card-title">{getMovieTitle(movie)}</span>
                      </Link>
                    );
                  })}
                </div>

                <button
                  type="button"
                  className="kaz-spotlight-nav kaz-spotlight-nav--next"
                  onClick={() => stepKazakhCarousel(1)}
                  aria-label="Следующий фильм"
                >
                  <ChevronRight size={18} />
                </button>
              </div>

              <div className="kaz-spotlight-panel">
                <span className="kaz-spotlight-label">KZ spotlight</span>
                <h3 className="kaz-spotlight-title">{getMovieTitle(activeKazakhMovie)}</h3>

                <div className="kaz-spotlight-meta">
                  {getMovieYear(activeKazakhMovie) ? <span>{getMovieYear(activeKazakhMovie)}</span> : null}
                  {formatRuntimeLabel(activeKazakhMovie) ? <span>{formatRuntimeLabel(activeKazakhMovie)}</span> : null}
                  {getMovieRating(activeKazakhMovie) ? (
                    <span>★ {getMovieRating(activeKazakhMovie).toFixed(1)}</span>
                  ) : null}
                </div>

                <p className="kaz-spotlight-description">
                  {getMovieSynopsis(activeKazakhMovie) || "Локальный хит с сильной подачей прямо в центре главной."}
                </p>

                <div className="kaz-spotlight-tags">
                  {getMovieGenres(activeKazakhMovie)
                    .slice(0, 3)
                    .map((genre) => (
                      <span key={`${getMovieId(activeKazakhMovie)}-${genre}`}>{genre}</span>
                    ))}
                </div>

                <Link to={`/movie/${getMovieId(activeKazakhMovie)}`} className="kaz-spotlight-link">
                  Открыть фильм
                  <ArrowRight size={16} />
                </Link>
              </div>
            </div>
          ) : null}
        </div>
      </section>

      {continueWatching.length > 0 ? (
        <section className="home-section">
          <div className="container">
            <div className="section-header">
              <h2 className="section-title">Продолжить просмотр</h2>
            </div>
            <MovieGrid movies={continueWatching} showProgress />
          </div>
        </section>
      ) : null}

      {userId && !isBecauseYouLikedError && personalizedItems.length > 0 ? (
        <MovieRail
          title={becauseYouLikedData?.method === "popular" ? "Популярное для вас" : "ИИ-подборка для вас"}
          items={personalizedItems}
        />
      ) : null}

      <MovieRail
        title="Новинки месяца"
        items={freshItems}
        linkTo="/genres"
        linkLabel="Все жанры"
      />

      <MovieRail
        title="Последние добавленные"
        items={recentlyAddedItems}
        linkTo="/genres"
        linkLabel="Все фильмы"
      />

      {topRatedItems.length > 0 ? (
        <section className="home-section">
          <div className="container home-rated-grid">
            <div className="section-header">
              <h2 className="section-title">Высокий рейтинг</h2>
              <Link to="/genres" className="section-link">
                Все фильмы
                <ArrowRight size={16} />
              </Link>
            </div>
            <MovieGrid movies={topRatedItems} />
          </div>
        </section>
      ) : null}

      {isLoading ? <div className="container section-state">Загрузка каталога...</div> : null}
      {isError ? <div className="container section-state error">Ошибка: {error.message}</div> : null}

      {q ? (
        <section className="home-section">
          <div className="container">
            <div className="section-header">
              <h2 className="section-title">Результаты поиска</h2>
            </div>
            {!isLoading ? <MovieGrid movies={filtered} /> : null}
          </div>
        </section>
      ) : null}
    </div>
  );
}
