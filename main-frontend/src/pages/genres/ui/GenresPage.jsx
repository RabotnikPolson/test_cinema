import React, { useState } from "react";
import { useGenres, useMovies } from "@/features/movies";
import { MovieGrid } from "@/shared/ui";
import "@/shared/styles/pages/Genres.css";

export default function GenresPage() {
  const { data: genres = [] } = useGenres();
  const { data: movies = [] } = useMovies();
  const [selected, setSelected] = useState(null);

  const filtered = selected
    ? movies.filter((movie) =>
        (movie.genre || "").toLowerCase().includes(selected.toLowerCase())
      )
    : movies;

  return (
    <div className="container genres-page">
      <h1>Жанры</h1>

      <div className="genres-row">
        {genres.map((genre) => (
          <button
            key={genre.id}
            className={`chip ${selected === genre.name ? "active" : ""}`}
            onClick={() => setSelected(selected === genre.name ? null : genre.name)}
          >
            {genre.name}
          </button>
        ))}
      </div>

      <MovieGrid movies={filtered} />
    </div>
  );
}
