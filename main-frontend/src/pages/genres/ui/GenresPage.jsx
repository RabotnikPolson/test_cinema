import React, { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { SlidersHorizontal, Star } from "lucide-react";
import { useGenres, useMovies } from "@/features/movies";
import { MovieGrid } from "@/shared/ui";
import {
  getMovieGenres,
  getMovieId,
  getMovieRating,
  getMovieSynopsis,
  getMovieTitle,
  getMovieYear,
} from "@/shared/lib/insight";
import "@/pages/genres/ui/Genres.css";

const SORT_OPTIONS = [
  { value: "rating", label: "По рейтингу" },
  { value: "year", label: "Сначала новые" },
  { value: "title", label: "По названию" },
];

function normalizeText(value) {
  return String(value || "").trim().toLowerCase();
}

export default function GenresPage() {
  const { data: genres = [], isLoading: isGenresLoading } = useGenres();
  const { data: movies = [], isLoading: isMoviesLoading, isError, error } = useMovies();
  const [searchParams, setSearchParams] = useSearchParams();
  const [selectedGenre, setSelectedGenre] = useState(searchParams.get("filter") || "");
  const [sortBy, setSortBy] = useState(searchParams.get("sort") || "rating");
  const [onlyKz, setOnlyKz] = useState(false);
  const query = searchParams.get("q") || "";

  useEffect(() => {
    const nextParams = new URLSearchParams(searchParams);

    if (selectedGenre) {
      nextParams.set("filter", selectedGenre);
    } else {
      nextParams.delete("filter");
    }

    if (sortBy && sortBy !== "rating") {
      nextParams.set("sort", sortBy);
    } else {
      nextParams.delete("sort");
    }

    const next = nextParams.toString();
    const current = searchParams.toString();

    if (next !== current) {
      setSearchParams(nextParams, { replace: true });
    }
  }, [searchParams, selectedGenre, setSearchParams, sortBy]);

  const genreCounts = useMemo(() => {
    const counts = new Map();

    movies.forEach((movie) => {
      getMovieGenres(movie).forEach((genre) => {
        const name = String(genre || "").trim();
        if (!name) {
          return;
        }

        counts.set(name, (counts.get(name) || 0) + 1);
      });
    });

    return counts;
  }, [movies]);

  const kazakhCount = useMemo(() => {
    return movies.filter((m) => m.raw?.isDomestic || m.raw?.domestic).length;
  }, [movies]);

  const hasCyrillic = (name) => /[а-яёА-ЯЁ]/.test(name);

  const genresWithCounts = useMemo(() => {
    return genres
      .map((genre) => ({
        ...genre,
        count: genreCounts.get(genre.name) || genre.movieCount || 0,
      }))
      .filter((genre) => !genre.name.includes("?") && hasCyrillic(genre.name))
      .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name));
  }, [genreCounts, genres]);

  const featuredGenres = genresWithCounts.slice(0, 6);

  const filteredMovies = useMemo(() => {
    const normalizedQuery = normalizeText(query);
    const normalizedSelectedGenre = normalizeText(selectedGenre);

    const nextMovies = movies.filter((movie) => {
      const title = normalizeText(getMovieTitle(movie));
      const genresText = normalizeText(getMovieGenres(movie).join(" "));
      const synopsis = normalizeText(getMovieSynopsis(movie));
      const yearText = String(getMovieYear(movie) || "");

      const matchesKz = onlyKz ? !!(movie.raw?.isDomestic || movie.raw?.domestic) : true;

      const matchesGenre = normalizedSelectedGenre
        ? getMovieGenres(movie).some((genre) => normalizeText(genre) === normalizedSelectedGenre)
        : true;

      const matchesQuery = normalizedQuery
        ? title.includes(normalizedQuery) ||
          genresText.includes(normalizedQuery) ||
          synopsis.includes(normalizedQuery) ||
          yearText.includes(normalizedQuery)
        : true;

      return matchesKz && matchesGenre && matchesQuery;
    });

    nextMovies.sort((left, right) => {
      if (sortBy === "title") {
        return getMovieTitle(left).localeCompare(getMovieTitle(right));
      }

      if (sortBy === "year") {
        return (getMovieYear(right) || 0) - (getMovieYear(left) || 0);
      }

      return (getMovieRating(right) || 0) - (getMovieRating(left) || 0);
    });

    return nextMovies;
  }, [movies, query, selectedGenre, sortBy, onlyKz]);

  const selectedGenreMeta = useMemo(() => {
    if (!selectedGenre) {
      return null;
    }

    return genresWithCounts.find((genre) => normalizeText(genre.name) === normalizeText(selectedGenre)) || null;
  }, [genresWithCounts, selectedGenre]);

  const highlightedMovie = filteredMovies[0] || null;
  const averageRating = useMemo(() => {
    const ratings = filteredMovies.map((movie) => getMovieRating(movie)).filter(Boolean);
    if (!ratings.length) {
      return null;
    }

    return ratings.reduce((sum, rating) => sum + rating, 0) / ratings.length;
  }, [filteredMovies]);

  const resetFilters = () => {
    setSelectedGenre("");
    setSortBy("rating");
    setOnlyKz(false);
    setSearchParams({}, { replace: true });
  };

  return (
    <div className="genres-page">
      <div className="container genres-shell">
        <section className="genres-hero">
          <div className="genres-hero-copy">
            <h1 className="genres-title">Все фильмы</h1>
            <p className="genres-lead">
              Полный каталог платформы. Фильтруйте по жанрам, сортируйте как удобно.
            </p>

            <div className="genres-stats">
              <div className="genres-stat">
                <span>Фильмов</span>
                <strong>{movies.length}</strong>
              </div>
              <div className="genres-stat">
                <span>Жанров</span>
                <strong>{genresWithCounts.length}</strong>
              </div>
              <div className="genres-stat">
                <span>Казахских</span>
                <strong>{kazakhCount}</strong>
              </div>
              <div className="genres-stat">
                <span>Средний рейтинг</span>
                <strong>{averageRating ? averageRating.toFixed(1) : "—"}</strong>
              </div>
            </div>
          </div>
        </section>

        <section className="genres-featured">
          {featuredGenres.map((genre, index) => (
            <button
              key={genre.id}
              type="button"
              className={`genre-feature-card${normalizeText(selectedGenre) === normalizeText(genre.name) ? " is-active" : ""}`}
              onClick={() =>
                setSelectedGenre((current) =>
                  normalizeText(current) === normalizeText(genre.name) ? "" : genre.name,
                )
              }
            >
              <span className="genre-feature-rank">0{index + 1}</span>
              <strong>{genre.name}</strong>
              <span>{genre.count} фильмов</span>
            </button>
          ))}
        </section>

        <section className="genres-toolbar">
          <div className="genres-toolbar-main">
            <div className="genres-toolbar-title">
              <SlidersHorizontal size={18} />
              <span>Фильтры каталога</span>
            </div>

            {(selectedGenre || sortBy !== "rating" || onlyKz || query) ? (
              <button type="button" className="genres-reset" onClick={resetFilters}>
                Сбросить фильтры
              </button>
            ) : null}
          </div>

          <div className="genres-controls">
            <div className="genres-sort-block">
              <span className="genres-control-label">Сортировка</span>
              <div className="genres-sort-pills">
                {SORT_OPTIONS.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    className={`genres-sort-pill${sortBy === option.value ? " is-active" : ""}`}
                    onClick={() => setSortBy(option.value)}
                  >
                    {option.label}
                  </button>
                ))}
                <button
                  type="button"
                  className={`genres-sort-pill${onlyKz ? " is-active" : ""}`}
                  onClick={() => setOnlyKz((v) => !v)}
                >
                  Только KZ
                </button>
              </div>
            </div>

            <div className="genres-chip-block">
              <span className="genres-control-label">Жанры</span>
              <div className="genres-chips">
                <button
                  type="button"
                  className={`genre-chip${!selectedGenre ? " is-active" : ""}`}
                  onClick={() => setSelectedGenre("")}
                >
                  Все
                </button>

                {genresWithCounts.map((genre) => (
                  <button
                    key={genre.id}
                    type="button"
                    className={`genre-chip${normalizeText(selectedGenre) === normalizeText(genre.name) ? " is-active" : ""}`}
                    onClick={() =>
                      setSelectedGenre((current) =>
                        normalizeText(current) === normalizeText(genre.name) ? "" : genre.name,
                      )
                    }
                  >
                    {genre.name}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </section>

        <section className="genres-results">
          <div className="genres-results-head">
            <div>
              <h2>{selectedGenreMeta?.name || "Весь каталог"}</h2>
              <p>
                Найдено <strong>{filteredMovies.length}</strong>
                {selectedGenreMeta ? ` в жанре ${selectedGenreMeta.name}` : ""}
                {query ? ` по запросу "${query}"` : ""}
              </p>
            </div>
          </div>

          {isMoviesLoading || isGenresLoading ? <div className="genres-state">Загрузка каталога...</div> : null}
          {isError ? <div className="genres-state genres-state--error">Ошибка: {error.message}</div> : null}

          {!isMoviesLoading && !isError && filteredMovies.length > 0 ? (
            <MovieGrid movies={filteredMovies} />
          ) : null}

          {!isMoviesLoading && !isError && filteredMovies.length === 0 ? (
            <div className="genres-empty">
              <h3>Ничего не найдено</h3>
              <p>Попробуйте убрать часть фильтров или изменить запрос в общем поиске сверху.</p>
              <button type="button" className="genres-reset genres-reset--solid" onClick={resetFilters}>
                Сбросить фильтры
              </button>
            </div>
          ) : null}
        </section>
      </div>
    </div>
  );
}
