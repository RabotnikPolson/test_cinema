import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useTrending } from "@/features/recommendations";
import { MovieGrid } from "@/shared/ui";
import { logClick } from "@/shared/api/metricsApi";
import { useAuth } from "@/features/auth";
import "./TrendingPage.css";

export default function TrendingPage() {
  const [activeTab, setActiveTab] = useState("all-time"); // "all-time" | "weekly"
  const { data, isLoading, isError } = useTrending(activeTab === "weekly");
  const navigate = useNavigate();
  const { user } = useAuth();

  const handleMovieClick = (movie) => {
    const movieId = movie.movie_id || movie.id;
    if (user?.id) {
      logClick(user.id, movieId, "trending_page");
    }
    navigate(`/movie/${movieId}`);
  };

  const movies = data?.recommendations || [];

  return (
    <div className="trending-page container" style={{ paddingTop: "2rem", paddingBottom: "2rem" }}>
      <div className="trending-header" style={{ marginBottom: "2rem" }}>
        <h1 style={{ marginBottom: "1rem" }}>В тренде</h1>
        <div className="trending-tabs" style={{ display: "flex", gap: "1rem" }}>
          <button
            className={`btn ${activeTab === "all-time" ? "btn--primary" : "btn--secondary"}`}
            onClick={() => setActiveTab("all-time")}
            type="button"
          >
            Сейчас популярно
          </button>
          <button
            className={`btn ${activeTab === "weekly" ? "btn--primary" : "btn--secondary"}`}
            onClick={() => setActiveTab("weekly")}
            type="button"
          >
            За неделю
          </button>
        </div>
      </div>

      {isLoading && <div className="loading">Загрузка...</div>}
      
      {isError && <div className="error">Ошибка загрузки трендов.</div>}
      
      {!isLoading && !isError && movies.length === 0 && (
        <div className="empty">Список пуст.</div>
      )}

      {!isLoading && !isError && movies.length > 0 && (
        <MovieGrid 
          movies={movies} 
          onMovieClick={handleMovieClick} 
        />
      )}
    </div>
  );
}
