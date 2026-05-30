import React, { useMemo } from "react";
import { RotateCcw } from "lucide-react";
import { getMovieId } from "@/shared/lib/insight";
import { getSavedMovieProgressPercent, useHistoryStorage } from "@/shared/utils";
import { MovieGrid } from "@/shared/ui";
import "@/pages/history/ui/History.css";

export default function HistoryPage() {
  const { read, clear } = useHistoryStorage();
  const raw = read();

  const items = useMemo(() => {
    return raw.map((item) => {
      const movieId = getMovieId(item);
      const progress = getSavedMovieProgressPercent(movieId);
      return { ...item, progress };
    });
  }, [raw]);

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

      <MovieGrid movies={items} showProgress />
    </div>
  );
}