import React, { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { addFromKinopoisk, deleteMovie, useMovies } from "@/features/movies";
import http from "@/shared/api/http-client";
import "@/pages/admin/ui/AddMovie.css";

const STATUS_COLORS = {
  none: { bg: "rgba(122,127,153,0.15)", color: "#707070" },
  pending: { bg: "rgba(201,168,76,0.15)", color: "#C9A84C" },
  in_progress: { bg: "rgba(201,168,76,0.2)", color: "#C9A84C" },
  completed: { bg: "rgba(34,197,94,0.15)", color: "#22c55e" },
  failed: { bg: "rgba(239,68,68,0.15)", color: "#ef4444" },
};

function StatusBadge({ status }) {
  const s = STATUS_COLORS[status] || STATUS_COLORS.none;
  return (
    <span
      style={{
        padding: "4px 10px",
        borderRadius: "999px",
        fontSize: "0.8rem",
        fontWeight: 600,
        background: s.bg,
        color: s.color,
        textTransform: "capitalize",
      }}
    >
      {status || "none"}
    </span>
  );
}

export default function AdminMoviesPage() {
  const { data: movies = [], isLoading, isError, error } = useMovies();
  const [kpId, setKpId] = useState("");
  const [genreName, setGenreName] = useState("");
  const [msg, setMsg] = useState("");
  const [toast, setToast] = useState("");
  const qc = useQueryClient();

  const addMut = useMutation({
    mutationFn: (id) => addFromKinopoisk(id),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ["movies"] });
      setMsg(`Фильм "${data?.title || `ID: ${kpId}`}" успешно добавлен`);
      setKpId("");
    },
    onError: (err) => {
      const errorMsg = err.response?.data?.message || err.message || "Ошибка сервера";
      setMsg(`Ошибка: ${errorMsg}`);
    },
  });

  const deleteMut = useMutation({
    mutationFn: (id) => deleteMovie(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["movies"] });
      setMsg("Фильм удален.");
    },
    onError: (err) => {
      const status = err.response?.status;
      const backend = err.response?.data?.message || err.response?.data || "Ошибка сервера";
      setMsg(`Ошибка удаления: ${status ? `[${status}] ` : ""}${backend}`);
    },
  });

  const triggerMut = useMutation({
    mutationFn: () => http.post("/api/test/subtitles/trigger-worker"),
    onSuccess: () => {
      setToast("Задача отправлена AI-агенту");
      setTimeout(() => setToast(""), 3000);
    },
    onError: () => {
      setToast("Ошибка запуска AI-воркера");
      setTimeout(() => setToast(""), 3000);
    },
  });

  const addGenreMut = useMutation({
    mutationFn: (name) => http.post("/api/genres", { name }),
    onSuccess: () => {
      setMsg(`Жанр "${genreName}" успешно добавлен`);
      setGenreName("");
    },
    onError: (err) => {
      const errorMsg = err.response?.data?.message || err.message || "Ошибка сервера";
      setMsg(`Ошибка добавления жанра: ${errorMsg}`);
    },
  });

  const onSubmit = (event) => {
    event.preventDefault();
    const id = kpId.trim();
    if (!/^[0-9]+$/.test(id)) {
      setMsg("Введите числовой Kinopoisk ID");
      return;
    }
    addMut.mutate(id);
  };

  const onGenreSubmit = (event) => {
    event.preventDefault();
    const name = genreName.trim();
    if (!name) {
      setMsg("Введите название жанра");
      return;
    }
    addGenreMut.mutate(name);
  };

  if (isLoading) {
    return <div className="container"><p>Загрузка списка фильмов...</p></div>;
  }

  if (isError) {
    return <div className="container"><p>Ошибка: {error?.message || "Не удалось получить фильмы"}</p></div>;
  }

  return (
    <div className="container addmovie-page">
      <h1>Админ: управление фильмами</h1>

      {/* Toast */}
      {toast && (
        <div
          style={{
            position: "fixed",
            top: 24,
            right: 24,
            zIndex: 9999,
            padding: "14px 24px",
            borderRadius: 12,
            background: "rgba(13,18,32,0.95)",
            backdropFilter: "blur(20px)",
            border: "1px solid rgba(201,168,76,0.3)",
            color: "#C9A84C",
            fontWeight: 600,
            fontSize: "0.9rem",
            boxShadow: "0 8px 32px rgba(0,0,0,0.5)",
          }}
        >
          {toast}
        </div>
      )}

      <section className="admin-add-section">
        <h2>Добавить фильм по Kinopoisk ID</h2>
        <form onSubmit={onSubmit} className="imdb-import">
          <input
            className="input"
            value={kpId}
            onChange={(event) => setKpId(event.target.value)}
            placeholder="Пример: 301"
          />
          <button className="button" disabled={addMut.isPending}>
            {addMut.isPending ? "Импорт..." : "Импортировать"}
          </button>
        </form>
      </section>

      <section className="admin-add-section" style={{ marginTop: '2rem' }}>
        <h2>Добавить новый жанр</h2>
        <form onSubmit={onGenreSubmit} className="imdb-import">
          <input
            className="input"
            value={genreName}
            onChange={(event) => setGenreName(event.target.value)}
            placeholder="Название жанра (например, Аниме)"
          />
          <button className="button" disabled={addGenreMut.isPending}>
            {addGenreMut.isPending ? "Добавление..." : "Добавить"}
          </button>
        </form>
      </section>

      {msg && (
        <p className={`status-message ${addMut.isError || deleteMut.isError ? "error" : "success"}`} role="status">
          {msg}
        </p>
      )}

      <section className="admin-list-section">
        <h2>Список фильмов</h2>
        {movies.length === 0 ? (
          <p>Фильмы не найдены.</p>
        ) : (
          <div className="movie-table-wrap">
            <table className="admin-movie-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Название</th>
                  <th>Год</th>
                  <th>Жанр</th>
                  <th>Действия</th>
                </tr>
              </thead>
              <tbody>
                {movies.map((movie) => (
                  <tr key={movie.id}>
                    <td>{movie.id}</td>
                    <td>{movie.title}</td>
                    <td>{movie.year || "-"}</td>
                    <td>{movie.genre || "-"}</td>
                    <td style={{ display: "flex", gap: 8 }}>
                      <button
                        className="button button--ghost"
                        disabled={deleteMut.isPending}
                        onClick={() => {
                          if (!window.confirm(`Удалить фильм "${movie.title}" (ID ${movie.id})?`)) return;
                          deleteMut.mutate(movie.id);
                        }}
                      >
                        Удалить
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

