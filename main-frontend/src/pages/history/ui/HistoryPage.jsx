import React, { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { RotateCcw } from "lucide-react";
import { getMovieId } from "@/shared/lib/insight";
import { getSavedMovieProgressPercent, useHistoryStorage } from "@/shared/utils";
import { MovieGrid } from "@/shared/ui";
import "@/pages/history/ui/History.css";

export default function HistoryPage() {
  const { t } = useTranslation();
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
            <div className="history-eyebrow">{t("history.eyebrow")}</div>
            <h1>{t("nav.history")}</h1>
          </div>
        </div>
        <div className="history-empty">
          <RotateCcw size={36} />
          <h2>{t("history.empty")}</h2>
          <p>{t("history.emptyHint")}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="container history-page">
      <div className="history-head">
        <div>
          <div className="history-eyebrow">{t("history.eyebrow")}</div>
          <h1>{t("nav.history")}</h1>
          <p>{t("history.recentCount", { count: items.length })}</p>
        </div>
        <button onClick={clear} className="history-clear" type="button">
          {t("history.clear")}
        </button>
      </div>

      <MovieGrid movies={items} showProgress />
    </div>
  );
}