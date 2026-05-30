import { getSavedMovieProgressPercent } from "@/shared/utils";

export function mapMovie(dto = {}) {
  const id = dto.id ?? null;

  const imdbId = dto.imdbId || dto.imdbID || null;
  const title = dto.title || dto.Title || "Без названия";
  const poster = dto.posterUrl || dto.Poster || null;
  const genre = (dto.genre && dto.genre.name) || dto.genreText || dto.Genre || "";
  const yearRaw = dto.year || dto.Year;
  const year =
    typeof yearRaw === "number"
      ? yearRaw
      : parseInt(String(yearRaw || "").replace(/\D/g, ""), 10) || null;

  const imdbRating = dto.imdbRating
    ? Number(dto.imdbRating)
    : dto.imdb_rating
      ? Number(dto.imdb_rating)
      : null;

  const runtime = dto.runtime || dto.Runtime || null;
  const description = dto.description || dto.Plot || dto.plot || "";
  const localProgress = id ? getSavedMovieProgressPercent(id) : 0;
  const progress =
    Number(dto.progress) ||
    Number(dto.watchProgress) ||
    Number(dto.watch_progress) ||
    localProgress;

  return {
    id,
    imdbId,
    title,
    nameOriginal: dto.nameOriginal || null,
    nameEn: dto.nameEn || null,
    poster,
    genre,
    year,
    imdbRating,
    runtime,
    description,
    progress,
    slogan: dto.slogan || null,
    shortDescription: dto.shortDescription || null,
    ratingKinopoisk: dto.ratingKinopoisk ? Number(dto.ratingKinopoisk) : null,
    ratingAge: dto.ratingAge || null,
    country: dto.country || null,
    contentType: dto.contentType || null,
    raw: dto,
  };
}
