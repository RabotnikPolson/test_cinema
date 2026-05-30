import React, { useMemo, useState } from "react";
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
  PointElement,
  LineElement,
  Filler
} from "chart.js";
import { Bar, Doughnut, Line } from "react-chartjs-2";
import "@/pages/admin/ui/AdminAnalytics.css";

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
  ArcElement,
  PointElement,
  LineElement,
  Filler
);
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
    delta: raw.delta || {},
  };
}

function normalizeScoreItems(items = []) {
  return (Array.isArray(items) ? items : []).map((item, index) => ({
    movieId: item.movieId ?? item.id ?? `${item.title || "item"}-${index}`,
    title: item.title || item.name || "Без названия",
    score: safeNumber(item.score ?? item.count ?? item.watchHours ?? item.rating),
    isDomestic: Boolean(item.isDomestic),
  }));
}

function normalizeQueue(raw = {}) {
  const rows = Array.isArray(raw.rows) ? raw.rows : [];
  return {
    pending: safeNumber(raw.pending),
    inProgress: safeNumber(raw.inProgress),
    success: safeNumber(raw.success),
    failed: safeNumber(raw.failed),
    rows: rows.map((row, index) => ({
      id: row.id ?? index + 1,
      movie: row.movie || row.title || "Без названия",
      lang: row.lang || row.language || "—",
      status: row.status || "pending",
      time: row.time || row.createdAt || "—",
    })),
  };
}

function normalizeRatio(raw = {}) {
  const domesticSeconds = safeNumber(raw.domesticWatchSeconds ?? raw.domestic);
  const foreignSeconds = safeNumber(raw.foreignWatchSeconds ?? raw.global);
  const total = domesticSeconds + foreignSeconds;
  const domesticPercent = total > 0 ? (domesticSeconds / total) * 100 : safeNumber(raw.domesticPercent);

  return {
    domesticWatchSeconds: domesticSeconds,
    foreignWatchSeconds: foreignSeconds,
    domesticPercent: domesticPercent || 0,
  };
}

function normalizeLegacyPayload(raw = {}) {
  const domestic = safeNumber(raw.domesticVsGlobal?.domestic);
  const foreign = safeNumber(raw.domesticVsGlobal?.global);

  return {
    source: "legacy",
    overview: normalizeOverview({
      totalUsers: raw.totalUsers,
      totalWatchHours: raw.totalViews,
      translatedSubtitles: Array.isArray(raw.subtitleLanguages) ? raw.subtitleLanguages.length : 0,
    }),
    topByClicks: normalizeScoreItems(raw.topSearchQueries?.map((item) => ({
      title: item.query,
      count: item.count,
    }))),
    topByWatchTime: [],
    topRatedMovies: [],
    topDomesticByClicks: [],
    contentRatio: normalizeRatio({ domestic, global: foreign }),
    subtitleQueue: normalizeQueue(),
  };
}

function normalizeDashboardPayload(raw = {}, source = "dashboard") {
  if (raw?.overview || raw?.contentRatio || raw?.subtitleQueue) {
    return {
      source,
      overview: normalizeOverview(raw.overview),
      topByClicks: normalizeScoreItems(raw.topByClicks),
      topByWatchTime: normalizeScoreItems(raw.topByWatchTime),
      topRatedMovies: normalizeScoreItems(raw.topRatedMovies),
      topDomesticByClicks: normalizeScoreItems(raw.topDomesticByClicks),
      contentRatio: normalizeRatio(raw.contentRatio),
      subtitleQueue: normalizeQueue(raw.subtitleQueue),
    };
  }

  return normalizeLegacyPayload(raw);
}

function formatShort(value) {
  if (typeof value !== "number") {
    return value;
  }

  if (value >= 1000) {
    return `${(value / 1000).toFixed(1).replace(".0", "")}K`;
  }

  return Math.round(value * 10) / 10;
}

function OverviewCards({ overview }) {
  const items = [
    { label: "Фильмов", value: overview.totalMovies, delta: overview.delta?.totalMovies },
    { label: "Казахстанских", value: overview.domesticMovies, unit: overview.totalMovies ? `/ ${overview.totalMovies}` : "" },
    { label: "Зарубежных", value: overview.foreignMovies, unit: overview.totalMovies ? `/ ${overview.totalMovies}` : "" },
    { label: "Пользователей", value: overview.totalUsers, delta: overview.delta?.totalUsers },
    { label: "Часов просмотра", value: overview.totalWatchHours, delta: overview.delta?.totalWatchHours },
    { label: "Субтитров", value: overview.translatedSubtitles, delta: overview.delta?.translatedSubtitles },
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
          {typeof item.delta === "number" ? (
            <span className={`ovcard__delta ${item.delta >= 0 ? "up" : "down"}`}>
              {item.delta >= 0 ? "▲" : "▼"} {Math.abs(item.delta)} за период
            </span>
          ) : null}
        </article>
      ))}
    </section>
  );
}

function BarList({ items, unit = "", topLimit = 10 }) {
  const visible = items.slice(0, topLimit);

  if (!visible.length) {
    return <div className="panel-empty">Данных пока недостаточно.</div>;
  }

  const data = {
    labels: visible.map(item => item.title.length > 25 ? item.title.substring(0, 25) + "..." : item.title),
    datasets: [
      {
        label: "Показатель",
        data: visible.map(item => item.score),
        backgroundColor: "#2A2A3A",
        hoverBackgroundColor: "#C9A84C",
        borderRadius: 2,
      },
    ],
  };

  const options = {
    indexAxis: 'y',
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
    datasets: [
      {
        data: [domesticPercent, 100 - domesticPercent],
        backgroundColor: ["#C9A84C", "#111111"],
        hoverBackgroundColor: ["#D4B150", "#2A2A3A"],
        borderColor: ["#111111", "#111111"],
        borderWidth: 2,
      },
    ],
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
        <div className="ratio__legend-row" style={{ display: "flex", alignItems: "center", gap: "0.5rem", color: "#9A9EB8" }}>
          <span className="ratio__dot" style={{ background: "#C9A84C", width: 10, height: 10, borderRadius: "50%" }} />
          Казахстанское
          <b style={{ marginLeft: "auto", color: "#E0E0E0", fontFamily: "JetBrains Mono" }}>{Math.round(ratio.domesticWatchSeconds / 3600)} ч</b>
        </div>
        <div className="ratio__legend-row" style={{ display: "flex", alignItems: "center", gap: "0.5rem", color: "#9A9EB8" }}>
          <span className="ratio__dot" style={{ background: "#111111", border: "1px solid #2A2A3A", width: 10, height: 10, borderRadius: "50%" }} />
          Зарубежное
          <b style={{ marginLeft: "auto", color: "#E0E0E0", fontFamily: "JetBrains Mono" }}>{Math.round(ratio.foreignWatchSeconds / 3600)} ч</b>
        </div>
      </div>
    </div>
  );
}

const STATUS_LABEL = {
  pending: { label: "В очереди", cls: "qs--pending" },
  progress: { label: "В работе", cls: "qs--progress" },
  in_progress: { label: "В работе", cls: "qs--progress" },
  success: { label: "Готово", cls: "qs--success" },
  completed: { label: "Готово", cls: "qs--success" },
  failed: { label: "Ошибка", cls: "qs--failed" },
};

function SubtitleQueue({ queue, onTriggerWorker, isPending }) {
  return (
    <div>
      <div className="queue">
        {queue.rows.length ? queue.rows.map((row) => {
          const status = STATUS_LABEL[row.status] || STATUS_LABEL.pending;
          return (
            <div className="queue__row" key={row.id}>
              <span className={`queue__status ${status.cls}`}>{status.label}</span>
              <div>
                <div className="queue__title">{row.movie}</div>
                <div className="queue__lang">{row.lang} · job #{row.id}</div>
              </div>
              <span className="queue__time">{row.time}</span>
            </div>
          );
        }) : <div className="panel-empty">Очередь пока пуста.</div>}
      </div>
      <div className="queue-actions">
        <button className="btn btn--primary" onClick={onTriggerWorker} disabled={isPending} type="button">
          <Rocket size={14} />
          {isPending ? "Запуск..." : "Запустить worker"}
        </button>
      </div>
    </div>
  );
}

function UserActivityLineChart({ activityData }) {
  if (!activityData || activityData.length === 0) {
    return <div className="panel-empty" style={{ height: 280, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>Нет данных</div>;
  }
  
  const data = {
    labels: activityData.map(d => d.label || ""),
    datasets: [
      {
        fill: true,
        label: "Активные пользователи",
        data: activityData.map(d => d.value || 0),
        borderColor: "#C9A84C",
        backgroundColor: (context) => {
          const chart = context.chart;
          const { ctx, chartArea } = chart;
          if (!chartArea) return null;
          const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
          gradient.addColorStop(0, "rgba(201, 168, 76, 0.3)");
          gradient.addColorStop(1, "rgba(201, 168, 76, 0)");
          return gradient;
        },
        tension: 0.4,
        pointBackgroundColor: "#111111",
        pointBorderColor: "#C9A84C",
        pointBorderWidth: 2,
      },
    ],
  };

  const options = {
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
      x: {
        grid: { display: false },
        ticks: { color: "#9A9EB8", font: { family: "DM Sans" } },
      },
      y: {
        grid: { color: "#111111" },
        ticks: { color: "#9A9EB8", font: { family: "DM Sans" } },
        border: { display: false },
      },
    },
  };

  return (
    <div style={{ height: 280 }}>
      <Line data={data} options={options} />
    </div>
  );
}

export default function AdminAnalyticsPage() {
  const [period, setPeriod] = useState("week");
  const [topLimit, setTopLimit] = useState(10);

  const aiHealthQuery = useQuery({
    queryKey: ["ai-health"],
    queryFn: async () => {
      const response = await http.get("/api/recommendations/trending");
      return response.data ? { status: "ok" } : { status: "unknown" };
    },
    refetchInterval: 30000,
  });

  const triggerRetrain = useMutation({
    mutationFn: () => http.post("/api/recommendations/retrain"),
    onSuccess: () => alert("Обучение запущено!"),
    onError: () => alert("Ошибка при запуске обучения")
  });

  const analyticsQuery = useQuery({
    queryKey: ["admin-analytics-dashboard", period, topLimit],
    queryFn: async () => {
      try {
        const response = await http.get("/admin/analytics/dashboard", {
          params: { period, topLimit },
        });
        return normalizeDashboardPayload(response.data, "dashboard");
      } catch (dashboardError) {
        const legacyResponse = await http.get("/api/admin/analytics");
        return normalizeDashboardPayload(legacyResponse.data, "legacy");
      }
    },
    staleTime: 1000 * 60 * 2,
  });

  const triggerWorker = useMutation({
    mutationFn: () => http.post("/api/test/subtitles/trigger-worker"),
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
            {data.source === "dashboard"
              ? `GET /admin/analytics/dashboard?period=${period}&topLimit=${topLimit}`
              : "Legacy fallback: GET /api/admin/analytics"}
          </p>
        </div>

        <div className="analytics-controls">
          <div className="seg">
            <button className={period === "week" ? "is-active" : ""} onClick={() => setPeriod("week")} type="button">
              неделя
            </button>
            <button className={period === "month" ? "is-active" : ""} onClick={() => setPeriod("month")} type="button">
              месяц
            </button>
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

      <section className="panel" style={{ marginBottom: "1.5rem", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <div>
          <h3 className="panel__title">Статус AI-модели</h3>
          {aiHealthQuery.isLoading ? (
            <span className="panel__sub">Загрузка статуса...</span>
          ) : aiHealthQuery.isError ? (
            <span className="panel__sub" style={{ color: "#e50914" }}>Модель недоступна</span>
          ) : (
            <span className="panel__sub">
              Статус: {aiHealthQuery.data?.status === "ok" ? "OK" : "ОШИБКА"} · Фильмов в памяти: {aiHealthQuery.data?.movies_in_memory || 0} · Казахстанских: {aiHealthQuery.data?.kazakhstan_movies || 0}
            </span>
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
          <BarList items={data.topByWatchTime} unit=" ч" topLimit={topLimit} />
        </section>

        <section className="panel">
          <div className="panel__head">
            <div>
              <h3 className="panel__title">Топ по рейтингам</h3>
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
            <span className="panel__sub">
              pending {data.subtitleQueue.pending} · in-progress {data.subtitleQueue.inProgress} · success {data.subtitleQueue.success} · failed {data.subtitleQueue.failed}
            </span>
          </div>
        </div>
        <SubtitleQueue
          queue={data.subtitleQueue}
          onTriggerWorker={() => triggerWorker.mutate()}
          isPending={triggerWorker.isPending}
        />
      </section>
    </div>
  );
}


