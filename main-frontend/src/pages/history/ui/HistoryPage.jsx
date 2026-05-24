import React from "react";
import { CheckCircle2, RotateCcw } from "lucide-react";
import { Link } from "react-router-dom";
import { getMoviePoster, getMovieYear } from "@/shared/lib/insight";
import { getSavedMovieProgressPercent, useHistoryStorage } from "@/shared/utils";
import "@/pages/history/ui/History.css";

export default function HistoryPage() {
  const { read, clear } = useHistoryStorage();
  const items = read();

  if (!items.length) {
    return (
      <div className="container history-page">
        <div className="history-head">
          <div>
            <div className="history-eyebrow">Лента просмотра</div>
            <h1>История</h1>
          </div>
        </div>
        <div className="history-empty">
          <RotateCcw size={36} />
          <h2>История пока пуста</h2>
          <p>Когда вы начнёте смотреть фильмы, последние сеансы появятся здесь.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="container history-page">
      <div className="history-head">
        <div>
          <div className="history-eyebrow">Лента просмотра</div>
          <h1>История</h1>
          <p>{items.length} последних открытий из вашего кинозала.</p>
        </div>
        <button onClick={clear} className="history-clear" type="button">
          Очистить
        </button>
      </div>

      <div className="history-list">
        {items.map((item) => {
          const progress = getSavedMovieProgressPercent(item.imdbId);
          const completed = progress === 0;

          return (
            <Link key={item.imdbId} to={`/movie/${item.imdbId}`} className="history-card">
              <div className="history-poster-wrap">
                <img src={getMoviePoster(item)} alt={item.title} className="history-poster" />
                <div className="history-progress">
                  <div style={{ width: `${progress}%` }} />
                </div>
              </div>
              <div className="history-meta">
                <div className="history-title-row">
                  <h3>{item.title}</h3>
                  {completed ? (
                    <span className="history-status history-status-done">
                      <CheckCircle2 size={14} />
                      Просмотрено
                    </span>
                  ) : (
                    <span className="history-status">{progress}%</span>
                  )}
                </div>
                <div className="history-subtitle">
                  {getMovieYear(item) || "Каталог"} · {new Date(item.timestamp).toLocaleString("ru-RU")}
                </div>
              </div>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
