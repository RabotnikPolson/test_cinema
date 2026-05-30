import { useState } from "react";
import {
  useKazakhstanMovies,
  useKazakhstanGenres,
} from "@/features/recommendations";
import KazakhstanFilters from "./KazakhstanFilters";
import KazakhstanMovieCard from "./KazakhstanMovieCard";
import "./KazakhstanSection.css";

function KazakhstanClassicsCarousel({ userId }) {
  const { data: moviesData, isLoading } = useKazakhstanMovies({
    userId,
    limit: 10,
    sortBy: "rating",
    yearTo: 2010,
  });

  const movies = moviesData?.recommendations || [];

  if (isLoading || movies.length === 0) return null;

  return (
    <div className="kz-classics-section">
      <h3 className="kz-classics-title">Бессмертная классика</h3>
      <div className="kz-movie-carousel">
        {movies.map((movie) => (
          <KazakhstanMovieCard key={movie.movie_id} movie={movie} />
        ))}
      </div>
    </div>
  );
}

export default function KazakhstanSection({ userId, showAll = false }) {
  const [filters, setFilters] = useState({
    genre: null,
    sortBy: "relevance",
  });

  const { data: genresData } = useKazakhstanGenres();
  const genres = genresData?.genres || [];

  const { data: moviesData, isLoading } = useKazakhstanMovies({
    userId,
    limit: showAll ? 40 : 20,
    genre: filters.genre,
    sortBy: filters.sortBy,
  });

  const movies = moviesData?.recommendations || [];

  return (
    <section className="kazakhstan-section" id="kazakhstan-section">
      <div className="kazakhstan-header">
        <span className="kz-flag-icon">KZ</span>
        <h2>Казахстанское кино</h2>
      </div>

      <KazakhstanClassicsCarousel userId={userId} />

      <KazakhstanFilters
        genres={genres}
        filters={filters}
        onChange={setFilters}
      />

      {isLoading ? (
        <div className="kz-loading">Загрузка</div>
      ) : movies.length === 0 ? (
        <div className="kz-empty-state">
          Нет фильмов по выбранным фильтрам. Попробуйте изменить параметры.
        </div>
      ) : (
        <div className="kz-movie-grid">
          {movies.map((movie) => (
            <KazakhstanMovieCard key={movie.movie_id} movie={movie} />
          ))}
        </div>
      )}
    </section>
  );
}
