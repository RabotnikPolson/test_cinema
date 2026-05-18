import React from "react";
import { useQuery } from "@tanstack/react-query";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
} from "chart.js";
import { Line, Pie, Bar } from "react-chartjs-2";
import http from "@/shared/api/http-client";
import "@/shared/styles/pages/AdminAnalytics.css";

ChartJS.register(
  CategoryScale, LinearScale, PointElement, LineElement,
  BarElement, ArcElement, Title, Tooltip, Legend
);

const chartDefaults = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { labels: { color: "#9A9EB8", font: { size: 12 } } },
  },
  scales: {
    x: { ticks: { color: "#7A7F99" }, grid: { color: "rgba(255,255,255,0.04)" } },
    y: { ticks: { color: "#7A7F99" }, grid: { color: "rgba(255,255,255,0.04)" } },
  },
};

const pieOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { position: "bottom", labels: { color: "#9A9EB8", padding: 16, font: { size: 12 } } },
  },
};

export default function AdminAnalyticsPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["admin-analytics"],
    queryFn: () => http.get("/api/admin/analytics").then((r) => r.data),
    staleTime: 1000 * 60 * 2,
  });

  if (isLoading) {
    return <div className="container analytics-page"><p>Загрузка аналитики...</p></div>;
  }

  if (isError || !data) {
    return <div className="container analytics-page"><p>Ошибка загрузки аналитики</p></div>;
  }

  // Line chart data
  const lineData = {
    labels: (data.viewsTrend || []).map((t) => t.date),
    datasets: [
      {
        label: "Просмотры",
        data: (data.viewsTrend || []).map((t) => t.count),
        borderColor: "#C9A84C",
        backgroundColor: "rgba(201,168,76,0.15)",
        fill: true,
        tension: 0.4,
        pointBackgroundColor: "#C9A84C",
        pointBorderColor: "#C9A84C",
      },
    ],
  };

  // Pie chart data
  const pieData = {
    labels: (data.subtitleLanguages || []).map((l) => l.language.toUpperCase()),
    datasets: [
      {
        data: (data.subtitleLanguages || []).map((l) => l.count),
        backgroundColor: ["#C9A84C", "#D4D8E8", "#7A5F28", "#9A9EB8", "#131325"],
        borderColor: "#0D0D1C",
        borderWidth: 2,
      },
    ],
  };

  // Bar chart data
  const barData = {
    labels: ["Казахстанское кино", "Глобальное кино"],
    datasets: [
      {
        label: "Просмотры",
        data: [data.domesticVsGlobal?.domestic || 0, data.domesticVsGlobal?.global || 0],
        backgroundColor: ["#C9A84C", "#7A7F99"],
        borderColor: ["#C9A84C", "#7A7F99"],
        borderWidth: 1,
        borderRadius: 6,
      },
    ],
  };

  return (
    <div className="container analytics-page">
      <h1>Аналитика</h1>

      {/* Stats cards */}
      <div className="analytics-stats">
        <div className="analytics-card glass">
          <span className="analytics-card-label">Всего просмотров</span>
          <span className="analytics-card-value">{data.totalViews}</span>
        </div>
        <div className="analytics-card glass">
          <span className="analytics-card-label">Пользователей</span>
          <span className="analytics-card-value">{data.totalUsers}</span>
        </div>
        <div className="analytics-card glass">
          <span className="analytics-card-label">Языков субтитров</span>
          <span className="analytics-card-value">{(data.subtitleLanguages || []).length}</span>
        </div>
      </div>

      {/* Charts grid */}
      <div className="analytics-grid">
        {/* Line Chart */}
        <div className="analytics-chart-card glass">
          <h3>Тренд просмотров (7 дней)</h3>
          <div className="analytics-chart-wrap">
            <Line data={lineData} options={chartDefaults} />
          </div>
        </div>

        {/* Pie Chart */}
        <div className="analytics-chart-card glass">
          <h3>Языки субтитров</h3>
          <div className="analytics-chart-wrap">
            <Pie data={pieData} options={pieOptions} />
          </div>
        </div>

        {/* Bar Chart */}
        <div className="analytics-chart-card glass">
          <h3>Domestic vs Global</h3>
          <div className="analytics-chart-wrap">
            <Bar data={barData} options={chartDefaults} />
          </div>
        </div>

        {/* Top Searches */}
        <div className="analytics-chart-card glass">
          <h3>Топ-5 поисковых запросов</h3>
          <div className="analytics-search-list">
            {(data.topSearchQueries || []).map((item, idx) => (
              <div key={idx} className="analytics-search-item">
                <span className="analytics-search-rank">#{idx + 1}</span>
                <span className="analytics-search-query">{item.query}</span>
                <span className="analytics-search-count">{item.count}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
