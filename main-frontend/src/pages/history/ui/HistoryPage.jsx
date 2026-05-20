import React from "react";
import { Link } from "react-router-dom";
import { useHistoryStorage } from "@/shared/utils";
import "@/shared/styles/pages/History.css";

export default function HistoryPage() {
  const { read, clear } = useHistoryStorage();
  const items = read();

  if (!items.length) {
    return (
      <div className="container history-page">
        <h1>История</h1>
        <p className="muted">Пока нет записей</p>
      </div>
    );
  }

  return (
    <div className="container history-page">
      <div className="header-row">
        <h1>История</h1>
        <button onClick={clear} className="button button--ghost">
          Очистить
        </button>
      </div>

      <div className="history-grid">
        {items.map((item) => (
          <Link key={item.imdbId} to={`/movie/${item.imdbId}`} className="history-card">
            <img
              src={
                item.posterUrl ||
                `https://placehold.jp/333/fff/200x300.png?text=${encodeURIComponent(item.title)}`
              }
              loading="lazy"
              width="120"
              height="180"
              alt={item.title}
            />
            <div className="meta">
              <h3>{item.title}</h3>
              <div className="ts">{new Date(item.timestamp).toLocaleString()}</div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
