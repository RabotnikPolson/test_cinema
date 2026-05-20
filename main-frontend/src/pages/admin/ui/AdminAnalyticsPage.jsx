import React, { useEffect, useState } from "react";
import http from "@/shared/api/http-client";
import "@/shared/styles/pages/AdminAnalytics.css";

export default function AdminAnalyticsPage() {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let mounted = true;

    http
      .get("/admin/analytics/daily")
      .then((response) => mounted && setData(response.data || []))
      .catch(() => mounted && setData([]))
      .finally(() => mounted && setLoading(false));

    return () => {
      mounted = false;
    };
  }, []);

  if (loading) {
    return (
      <div className="container analytics-page">
        <p>Загрузка...</p>
      </div>
    );
  }

  return (
    <div className="container analytics-page">
      <h1>Аналитика по дням</h1>
      <div className="table-wrap">
        <table className="analytics-table">
          <thead>
            <tr>
              <th>Пользователь</th>
              <th>Дата</th>
              <th>Просмотры</th>
              <th>Лайки</th>
              <th>Поделились</th>
            </tr>
          </thead>
          <tbody>
            {data.map((item) => (
              <tr key={item.id}>
                <td>{item.userId}</td>
                <td>{item.d}</td>
                <td>{item.plays}</td>
                <td>{item.likes}</td>
                <td>{item.shares}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
