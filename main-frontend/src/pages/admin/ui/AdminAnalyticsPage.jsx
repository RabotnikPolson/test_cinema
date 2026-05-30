import React, { useState } from "react";
import { RefreshCw, Rocket } from "lucide-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import http from "@/shared/api/http-client";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ArcElement,
} from "chart.js";
import { Bar, Doughnut } from "react-chartjs-2";
import "@/pages/admin/ui/AdminAnalytics.css";

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend, ArcElement);

function safeNumber(value) {
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
}

function normalizeOverview(raw = {}) {
  return {
    totalMovies: safeNumber(raw.totalMovies),
    domesticMovies: safeNumber(raw.domesticMovies),
    foreignMovies: safeNumber(raw.foreignMovies),
    totalUsers: safeNumber(raw.totalUsers),
    totalWatchHours: safeNumber(raw.totalWatchHours),
    translatedSubtitles: safeNumber(raw.translatedSubtitles),
  };
}

function normalizeScoreItems(items = []) {
  return (Array.isArray(items) ? items : []).map((item, index) => ({
    movieId: item.movieId ?? item.id ?? index,
    title: item.title || item.name || "Без названия",
    score: safeNumber(item.score ?? item.count ?? item.watchHours ?? item.rating),
    isDomestic: Boolean(item.isDomestic),
  }));
}

function normalizeQueue(raw = {}) {
  return {
    pending: safeNumber(raw.pending),
    inProgress: safeNumber(raw.inProgress),
    success: safeNumber(raw.success),
    failed: safeNumber(raw.failed),
    total: safeNumber(raw.total),
  };
}

function normalizeRatio(raw = {}) {
  const domesticSeconds = safeNumber(raw.domesticWatchSeconds ?? raw.domestic);
  const foreignSeconds = safeNumber(raw.foreignWatchSeconds ?? raw.global);
  const total = domesticSeconds + foreignSeconds;
  const domesticPercent = total > 0
    ? (domesticSeconds / total) * 100
    : safeNumber(raw.domesticPercent);
  return { domesticWatchSeconds: domesticSeconds, foreignWatchSeconds: foreignSeconds, domesticPercent };
}

function normalizeDashboard(raw = {}) {
  return {
    overview: normalizeOverview(raw.overview),
    topByClicks: normalizeScoreItems(raw.topByClicks),
    topByWatchTime: normalizeScoreItems(raw.topByWatchTime),
    topRatedMovies: normalizeScoreItems(raw.topRatedMovies),
    topDomesticByClicks: normalizeScoreItems(raw.topDomesticByClicks),
    contentRatio: normalizeRatio(raw.contentRatio),
    subtitleQueue: normalizeQueue(raw.subtitleQueue),
  };
}

function formatShort(value) {
  if (typeof value !== "number") return value;
  if (value >= 1000) return `${(value / 1000).toFixed(1).replace(".0", "")}K`;
  return Math.round(value * 10) / 10;
}

function OverviewCards({ overview }) {
  const items = [
    { label: "Фильмов", value: overview.totalMovies },
    { label: "Казахстанских", value: overview.domesticMovies, unit: overview.totalMovies ? `/ ${overview.totalMovies}` : "" },
    { label: "Зарубежных", value: overview.foreignMovies, unit: overview.totalMovies ? `/ ${overview.totalMovies}` : "" },
    { label: "Пользователей", value: overview.totalUsers },
    { label: "Часов просмотра", value: overview.totalWatchHours },
    { label: "Субтитров KZ", value: overview.translatedSubtitles },
  ];

  return (
    <section className="ovgrid">
      {items.map((item) => (
        <article className="ovcard" key={item.label}>
          <span className="ovcard__label">{item.label}</span>
          <div>
            <span className="ovcard__value">{formatShort(item.value)}</span>
            {item.unit ? <span className="ovcard__unit">{item.unit}</span> : null}
          </div>
        </article>
      ))}
    </section>
  );
}

function BarList({ items, topLimit = 10 }) {
  const visible = items.slice(0, topLimit);
  if (!visible.length) return <div className="panel-empty">Данных пока недостаточно.</div>;

  const data = {
    labels: visible.map((item) =>
      item.title.length > 25 ? item.title.substring(0, 25) + "…" : item.title
    ),
    datasets: [{
      label: "Показатель",
      data: visible.map((item) => item.score),
      backgroundColor: "#2A2A3A",
      hoverBackgroundColor: "#C9A84C",
      borderRadius: 2,
    }],
  };

  const options = {
    indexAxis: "y",
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: "#111111",
        titleColor: "#9A9EB8",
        bodyColor: "#E0E0E0",
        borderColor: "#2A2A3A",
        borderWidth: 1,
      },
    },
    scales: {
      x: { display: false },
      y: {
        ticks: { color: "#9A9EB8", font: { family: "DM Sans" } },
        grid: { display: false },
        border: { display: false },
      },
    },
  };

  return (
    <div style={{ height: 320 }}>
      <Bar data={data} options={options} />
    </div>
  );
}

function ContentRatioPie({ ratio }) {
  const domesticPercent = safeNumber(ratio.domesticPercent);
  const data = {
    labels: ["Казахстанское", "Зарубежное"],
    datasets: [{
      data: [domesticPercent, 100 - domesticPercent],
      backgroundColor: ["#C9A84C", "#111111"],
      hoverBackgroundColor: ["#D4B150", "#2A2A3A"],
      borderColor: ["#111111", "#111111"],
      borderWidth: 2,
    }],
  };

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: "75%",
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: "#111111",
        titleColor: "#9A9EB8",
        bodyColor: "#E0E0E0",
        borderColor: "#2A2A3A",
        borderWidth: 1,
      },
    },
  };

  return (
    <div className="ratio" style={{ flexDirection: "column" }}>
      <div style={{ width: 180, height: 180, position: "relative" }}>
        <Doughnut data={data} options={options} />
        <div style={{ position: "absolute", inset: 0, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", pointerEvents: "none" }}>
          <span style={{ fontFamily: "Bebas Neue", fontSize: "28px", color: "#E0E0E0" }}>{domesticPercent.toFixed(1)}%</span>
          <span style={{ fontSize: "10px", color: "#9A9EB8", letterSpacing: "0.2em", textTransform: "uppercase" }}>KZ</span>
        </div>
      </div>
      <div className="ratio__legend" style={{ marginTop: "1rem", width: "100%", display: "grid", gap: "0.5rem" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", color: "#9A9EB8" }}>
          <span style={{ background: "#C9A84C", width: 10, height: 10, borderRadius: "50%", flexShrink: 0 }} />
          Казахстанское
          <b style={{ marginLeft: "auto", color: "#E0E0E0", fontFamily: "JetBrains Mono" }}>
            {Math.round(ratio.domesticWatchSeconds / 3600)} ч
          </b>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", color: "#9A9EB8" }}>
          <span style={{ background: "#111111", border: "1px solid #2A2A3A", width: 10, height: 10, borderRadius: "50%", flexShrink: 0 }} />
          Зарубежное
          <b style={{ marginLeft: "auto", color: "#E0E0E0", fontFamily: "JetBrains Mono" }}>
            {Math.round(ratio.foreignWatchSeconds / 3600)} ч
          </b>
        </div>
      </div>
    </div>
  );
}

const QUEUE_STATS = [
  { key: "pending",    label: "В очереди",  color: "#9A9EB8" },
  { key: "inProgress", label: "В работе",   color: "#C9A84C" },
  { key: "success",    label: "Готово",     color: "#22c55e" },
  { key: "failed",     label: "Ошибка",     color: "#ef4444" },
];

const QUEUE_ROW_STATUS = {
  in_progress: { label: "В работе",  color: "#C9A84C" },
  pending:     { label: "В очереди", color: "#9A9EB8" },
  none:        { label: "Ожидает",   color: "#9A9EB8" },
  failed:      { label: "Ошибка",    color: "#ef4444" },
  success:     { label: "Готово",    color: "#22c55e" },
};

function SubtitleQueue({ queue, rows = [], onTriggerWorker, isPending }) {
  return (
    <div>
      <div className="queue-counters">
        {QUEUE_STATS.map(({ key, label, color }) => (
          <div key={key} className="queue-counter">
            <span className="queue-counter__value" style={{ color }}>{queue[key]}</span>
            <span className="queue-counter__label">{label}</span>
          </div>
        ))}
        <div className="queue-counter">
          <span className="queue-counter__value" style={{ color: "#E0E0E0" }}>{queue.total}</span>
          <span className="queue-counter__label">Всего</span>
        </div>
      </div>

      {rows.length > 0 ? (
        <div className="queue-rows">
          {rows.map((row) => {
            const st = QUEUE_ROW_STATUS[row.status] || QUEUE_ROW_STATUS.none;
            return (
              <div key={row.id} className="queue-row">
                <span className="queue-row__status" style={{ color: st.color }}>{st.label}</span>
                <span className="queue-row__title">{row.movieTitle}</span>
                <span className="queue-row__lang">{row.language}</span>
                {row.linesTranslated > 0 ? (
                  <span className="queue-row__lines">{row.linesTranslated} строк</span>
                ) : null}
              </div>
            );
          })}
        </div>
      ) : (
        <div className="panel-empty">Нет скачанных субтитров в очереди.</div>
      )}

      <div className="queue-actions">
        <button className="btn btn--primary" onClick={onTriggerWorker} disabled={isPending} type="button">
          <Rocket size={14} />
          {isPending ? "Запуск..." : "Запустить worker"}
        </button>
      </div>
    </div>
  );
}

export default function AdminAnalyticsPage() {
  const [period, setPeriod] = useState("week");
  const [topLimit, setTopLimit] = useState(10);
  const [retrainMsg, setRetrainMsg] = useState("");

  const analyticsQuery = useQuery({
    queryKey: ["admin-analytics-dashboard", period, topLimit],
    queryFn: async () => {
      const res = await http.get("/admin/analytics/dashboard", { params: { period, topLimit } });
      return normalizeDashboard(res.data);
    },
    staleTime: 1000 * 60 * 2,
  });

  const triggerRetrain = useMutation({
    mutationFn: () => http.post("/api/recommendations/retrain"),
    onSuccess: () => {
      setRetrainMsg("Переобучение запущено");
      setTimeout(() => setRetrainMsg(""), 4000);
    },
    onError: () => {
      setRetrainMsg("Ошибка при запуске переобучения");
      setTimeout(() => setRetrainMsg(""), 4000);
    },
  });

  const subtitleRowsQuery = useQuery({
    queryKey: ["subtitle-queue-rows"],
    queryFn: async () => {
      const res = await http.get("/admin/analytics/subtitle-queue");
      return Array.isArray(res.data) ? res.data : [];
    },
    staleTime: 1000 * 30,
  });

  const triggerWorker = useMutation({
    mutationFn: () => http.post("/api/test/subtitles/trigger-worker"),
    onSuccess: () => subtitleRowsQuery.refetch(),
  });

  const data = analyticsQuery.data;

  if (analyticsQuery.isLoading) {
    return <div className="container analytics-loading">Загрузка аналитики...</div>;
  }

  if (analyticsQuery.isError || !data) {
    return <div className="container analytics-loading">Не удалось загрузить аналитику.</div>;
  }

  return (
    <div className="analytics-dashboard">
      <header className="analytics-topbar">
        <div>
          <h1 className="analytics-title">Аналитика платформы</h1>
          <p className="analytics-subtitle">
            GET /admin/analytics/dashboard?period={period}&topLimit={topLimit}
          </p>
        </div>

        <div className="analytics-controls">
          <div className="seg">
            <button className={period === "week" ? "is-active" : ""} onClick={() => setPeriod("week")} type="button">неделя</button>
            <button className={period === "month" ? "is-active" : ""} onClick={() => setPeriod("month")} type="button">месяц</button>
          </div>
          <div className="seg">
            {[5, 10, 20].map((value) => (
              <button
                key={value}
                className={topLimit === value ? "is-active" : ""}
                onClick={() => setTopLimit(value)}
                type="button"
              >
                top {value}
              </button>
            ))}
          </div>
          <button className="btn btn--secondary" onClick={() => analyticsQuery.refetch()} type="button">
            <RefreshCw size={14} />
            Обновить
          </button>
        </div>
      </header>

      <section className="panel" style={{ marginBottom: "1.5rem", display: "flex", justifyContent: "space-between", alignItems: "center", gap: "1rem" }}>
        <div>
          <h3 className="panel__title">AI рекомендательная модель</h3>
          {retrainMsg ? (
            <span className="panel__sub" style={{ color: retrainMsg.startsWith("Ошибка") ? "#ef4444" : "#22c55e" }}>
              {retrainMsg}
            </span>
          ) : (
            <span className="panel__sub">Переобучение обновит матрицу схожести на основе текущих данных</span>
          )}
        </div>
        <button
          className="btn btn--primary"
          onClick={() => triggerRetrain.mutate()}
          disabled={triggerRetrain.isPending}
          type="button"
        >
          {triggerRetrain.isPending ? "Запуск..." : "Переобучить модель"}
        </button>
      </section>

      <OverviewCards overview={data.overview} />

      <div className="row3">
        <section className="panel">
          <div className="panel__head">
            <div>
              <h3 className="panel__title">Топ по кликам</h3>
              <span className="panel__sub">topByClicks · {topLimit}</span>
            </div>
          </div>
          <BarList items={data.topByClicks} topLimit={topLimit} />
        </section>

        <section className="panel">
          <div className="panel__head">
            <div>
              <h3 className="panel__title">Топ по просмотрам</h3>
              <span className="panel__sub">topByWatchTime · часы</span>
            </div>
          </div>
          <BarList items={data.topByWatchTime} topLimit={topLimit} />
        </section>

        <section className="panel">
          <div className="panel__head">
            <div>
              <h3 className="panel__title">Топ по рейтингу</h3>
              <span className="panel__sub">topRatedMovies · /10</span>
            </div>
          </div>
          <BarList items={data.topRatedMovies} topLimit={topLimit} />
        </section>
      </div>

      <div className="row2">
        <section className="panel">
          <div className="panel__head">
            <div>
              <h3 className="panel__title">Топ казахстанских фильмов</h3>
              <span className="panel__sub">topDomesticByClicks</span>
            </div>
          </div>
          <BarList items={data.topDomesticByClicks} topLimit={topLimit} />
        </section>

        <section className="panel">
          <div className="panel__head">
            <div>
              <h3 className="panel__title">Казахстанский vs зарубежный</h3>
              <span className="panel__sub">contentRatio · часы просмотра</span>
            </div>
          </div>
          <ContentRatioPie ratio={data.contentRatio} />
        </section>
      </div>

      <section className="panel">
        <div className="panel__head">
          <div>
            <h3 className="panel__title">Очередь субтитров</h3>
            <span className="panel__sub">movie_subtitles · is_downloaded = true</span>
          </div>
        </div>
        <SubtitleQueue
          queue={data.subtitleQueue}
          rows={subtitleRowsQuery.data || []}
          onTriggerWorker={() => triggerWorker.mutate()}
          isPending={triggerWorker.isPending}
        />
      </section>
    </div>
  );
}
