import React from "react";
import { Link } from "react-router-dom";
import { useRecommendationsTab } from "@/features/recommendations/model/useRecommendations";

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
          {items.map((item) => {
            const posterSrc =
              item.poster_url ||
              `https://placehold.jp/333/fff/300x450.png?text=${encodeURIComponent(
                item.title
              )}`;

            return (
              <Link
                key={item.movie_id}
                to={`/movie/${item.movie_id}/watch`}
                className="rail-item glass"
                onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
              >
                <div className="rail-item-thumb">
                  <img src={posterSrc} alt={item.title} loading="lazy" />
                  <div className="rail-item-hover">
                    <svg viewBox="0 0 24 24" fill="currentColor">
                      <path d="M8 5v14l11-7z" />
                    </svg>
                  </div>
                </div>
                <div className="rail-item-info">
                  <strong>{item.title}</strong>
                  <span>{item.year || item.release_year || "—"}</span>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
