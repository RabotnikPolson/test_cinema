import backdropCinema from "@/shared/assets/insight/backdrop-cinema.svg";
import posterPlaceholder from "@/shared/assets/insight/poster-placeholder.svg";

export function getMovieId(movie) {
  return movie?.id ?? movie?.movieId ?? movie?.imdbId ?? movie?.movie_id ?? null;
}

export function getMovieTitle(movie) {
  return movie?.title || movie?.originalTitle || movie?.raw?.title || "Без названия";
}

export function getMoviePoster(movie) {
  return (
    movie?.poster ||
    movie?.posterUrl ||
    movie?.poster_url ||
    movie?.raw?.posterUrl ||
    movie?.raw?.Poster ||
    posterPlaceholder
  );
}

export function getMovieBackdrop(movie) {
  return (
    movie?.backdrop ||
    movie?.backdropUrl ||
    movie?.backdrop_url ||
    movie?.raw?.backdrop ||
    movie?.raw?.backdropUrl ||
    movie?.coverUrl ||
    movie?.raw?.coverUrl ||
    getMoviePoster(movie) ||
    backdropCinema
  );
}

export function getMovieYear(movie) {
  const rawYear = movie?.year || movie?.releaseDate || movie?.raw?.year || movie?.raw?.Year;
  if (!rawYear) {
    return null;
  }

  if (typeof rawYear === "number") {
    return rawYear;
  }

  const match = String(rawYear).match(/\d{4}/);
  return match ? Number(match[0]) : null;
}

export function getMovieRating(movie) {
  const rawRating =
    movie?.ratingKinopoisk ??
    movie?.raw?.ratingKinopoisk ??
    movie?.rating ??
    movie?.imdbRating ??
    movie?.raw?.rating ??
    movie?.raw?.imdbRating ??
    movie?.raw?.imdb_rating;

  const value = Number(rawRating);
  return Number.isFinite(value) && value > 0 ? value : null;
}

export function getMovieRuntimeMinutes(movie) {
  const rawRuntime = movie?.runtime ?? movie?.raw?.runtime ?? movie?.raw?.Runtime;
  if (typeof rawRuntime === "number" && Number.isFinite(rawRuntime)) {
    return rawRuntime;
  }

  const match = String(rawRuntime || "").match(/\d+/);
  return match ? Number(match[0]) : null;
}

export function formatRuntimeLabel(runtime) {
  const minutes = typeof runtime === "object" ? getMovieRuntimeMinutes(runtime) : Number(runtime);
  if (!minutes || !Number.isFinite(minutes)) {
    return null;
  }

  const hours = Math.floor(minutes / 60);
  const restMinutes = minutes % 60;

  if (!hours) {
    return `${minutes} мин`;
  }

  if (!restMinutes) {
    return `${hours} ч`;
  }

  return `${hours} ч ${restMinutes} мин`;
}

export function getMovieGenres(movie) {
  if (Array.isArray(movie?.genres)) {
    return movie.genres
      .map((genre) => (typeof genre === "string" ? genre : genre?.name))
      .filter(Boolean);
  }

  const rawGenres = movie?.raw?.genres;
  if (Array.isArray(rawGenres)) {
    return rawGenres.map((genre) => genre?.name || genre).filter(Boolean);
  }

  if (movie?.genre) {
    return String(movie.genre)
      .split(",")
      .map((genre) => genre.trim())
      .filter(Boolean);
  }

  return [];
}

export function getMovieSynopsis(movie) {
  return movie?.description || movie?.plot || movie?.raw?.Plot || movie?.raw?.description || "";
}

export function getMovieAgeLabel(movie) {
  return movie?.age || movie?.raw?.ageRating || movie?.raw?.rated || null;
}

export function getInitials(value) {
  if (!value) {
    return "U";
  }

  const parts = String(value).trim().split(/\s+/).filter(Boolean);
  if (parts.length >= 2) {
    return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
  }

  return String(value).trim().slice(0, 2).toUpperCase() || "U";
}

export function formatActivityDate(value) {
  if (!value) {
    return "Недавно";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Недавно";
  }

  return date.toLocaleString("ru-RU", {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
}

