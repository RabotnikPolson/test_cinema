import React from "react";
import { MovieCard } from "@/entities/movie";
import { getMovieId } from "@/shared/lib/insight";

export default function MovieGrid({ movies = [], showProgress = false, onMovieClick }) {
  if (!movies.length) {
    return <div className="empty-state">Контент не найден</div>;
  }

  return (
    <div className="movie-grid">
      {movies.map((movie) => (
        <MovieCard
          key={getMovieId(movie) ?? movie?.title}
          movie={movie}
          showProgress={showProgress}
          onClick={onMovieClick ? () => onMovieClick(movie) : undefined}
        />
      ))}
    </div>
  );
}
