import React from "react";
import { Link } from "react-router-dom";
import { useRecommendationsTab } from "@/features/recommendations/model/useRecommendations";
import { getMovieId, getMoviePoster, getMovieTitle, getMovieYear } from "@/shared/lib/insight";

const rails = [
  {
    type: "franchise",
    title: "Продолжение",
    description: "Следующие и предыдущие части",
  },
  {
    type: "content",
    title: "Похожие",
    description: "Фильмы с похожим сюжетом",
  },
  {
    type: "hybrid",
    title: "Рекомендации",
    description: "Специально для вас",
  },
];

export default function WatchRecommendationsRail({ movieId }) {
  return (
    <div className="watch-rails">
      {rails.map((rail) => (
        <RecommendationRail key={rail.type} rail={rail} movieId={movieId} />
      ))}
    </div>
  );
}

function RecommendationRail({ rail, movieId }) {
  const { data, isLoading } = useRecommendationsTab(rail.type, movieId, 6);
  const items = data?.recommendations || [];

  if (!isLoading && items.length === 0) {
    return null;
  }

  return (
    <div className="watch-rail-block">
      <div className="rail-header">
        <h4>{rail.title}</h4>
        <p>{rail.description}</p>
      </div>

      {isLoading ? (
        <div className="rail-empty">Загрузка...</div>
      ) : (
        <div className="rail-list no-scrollbar">
          {items.map((item) => (
            <Link
              key={getMovieId(item)}
              to={`/movie/${getMovieId(item)}`}
              className="rail-item glass"
              onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
            >
              <div className="rail-item-thumb">
                <img src={getMoviePoster(item)} alt={getMovieTitle(item)} loading="lazy" />
                <div className="rail-item-hover">
                  <svg viewBox="0 0 24 24" fill="currentColor">
                    <path d="M8 5v14l11-7z" />
                  </svg>
                </div>
              </div>
              <div className="rail-item-info">
                <strong>{getMovieTitle(item)}</strong>
                <span>{getMovieYear(item) || "—"}</span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
