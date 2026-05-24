// src/components/MovieGrid.jsx
import React from "react";
import { MovieCard } from "@/entities/movie";

export default function MovieGrid({ movies = [], showProgress = false, onMovieClick }) {
  if (!movies.length) return <div className="empty-state">Контент не найден</div>;
  return (
    <div className="movie-grid">
      {movies.map(m => (
        <MovieCard key={m.id ?? m.imdbId ?? m.title} movie={m} showProgress={showProgress} onClick={onMovieClick ? () => onMovieClick(m) : undefined} />
      ))}
    </div>
  );
}