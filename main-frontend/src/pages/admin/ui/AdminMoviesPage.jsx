import React, { useMemo, useState } from "react";
import { Upload } from "lucide-react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { addFromKinopoisk, deleteMovie, useMovies } from "@/features/movies";
import { getMovieGenres } from "@/shared/lib/insight";
import http from "@/shared/api/http-client";
import "@/pages/admin/ui/AddMovie.css";

function parseBulkIds(value) {
  return value
    .split(/[\s,]+/)
    .map((s) => s.trim())
    .filter(Boolean);
}

const STATUS_LABEL = {
  success: "Добавлен",
  already_exists: "Уже есть",
  error: "Ошибка",
};

const STATUS_COLOR = {
  success: "#22c55e",
  already_exists: "#C9A84C",
  error: "#ef4444",
};

export default function AdminMoviesPage() {
  const { data: movies = [], isLoading, isError, error } = useMovies();
  const qc = useQueryClient();

  // single import
  const [kpId, setKpId] = useState("");
  const [singleMsg, setSingleMsg] = useState("");

  // bulk import
  const [bulkText, setBulkText] = useState("");
  const [bulkResults, setBulkResults] = useState(null);

  // movie list
  const [search, setSearch] = useState("");
  const [deleteMsg, setDeleteMsg] = useState("");

  const bulkIds = useMemo(() => parseBulkIds(bulkText), [bulkText]);

  const addMut = useMutation({
    mutationFn: (id) => addFromKinopoisk(id),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ["movies"] });
      setSingleMsg(`Добавлен: "${data?.title || `ID ${kpId}`}"`);
      setKpId("");
    },
    onError: (err) => {
      setSingleMsg(`Ошибка: ${err.response?.data?.message || err.message || "Ошибка сервера"}`);
    },
  });

  const bulkMut = useMutation({
    mutationFn: (ids) => http.post("/movies/bulkImport", { kinopoiskIds: ids }).then((r) => r.data),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ["movies"] });
      setBulkResults(data);
      setBulkText("");
    },
    onError: (err) => {
      setBulkResults({ total: 0, success: 0, failed: bulkIds.length, results: [], error: err.response?.data?.message || err.message });
    },
  });

  const deleteMut = useMutation({
    mutationFn: (id) => deleteMovie(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["movies"] });
      setDeleteMsg("Фильм удалён.");
    },
    onError: (err) => {
      const status = err.response?.status;
      const backend = err.response?.data?.message || err.response?.data || "Ошибка сервера";
      setDeleteMsg(`Ошибка удаления: ${status ? `[${status}] ` : ""}${backend}`);
    },
  });

  const onSingleSubmit = (event) => {
    event.preventDefault();
    const id = kpId.trim();
    if (!/^\d+$/.test(id)) { setSingleMsg("Введите числовой Kinopoisk ID"); return; }
    setSingleMsg("");
    addMut.mutate(id);
  };

  const onBulkSubmit = (event) => {
    event.preventDefault();
    if (!bulkIds.length) return;
    const invalid = bulkIds.find((id) => !/^\d+$/.test(id));
    if (invalid) { setBulkResults({ error: `Некорректный ID: ${invalid}` }); return; }
    setBulkResults(null);
    bulkMut.mutate(bulkIds);
  };

  const filteredMovies = search.trim()
    ? movies.filter((m) => {
        const q = search.toLowerCase();
        return (
          String(m.id).includes(q) ||
          (m.title || "").toLowerCase().includes(q) ||
          getMovieGenres(m).join(" ").toLowerCase().includes(q)
        );
      })
    : movies;

  if (isLoading) return <div className="addmovie-shell"><p>Загрузка...</p></div>;
  if (isError) return <div className="addmovie-shell"><p>Ошибка: {error?.message}</p></div>;

  return (
    <div className="addmovie-shell">
      <div className="addmovie-hero">
        <div className="addmovie-eyebrow">Admin</div>
        <h1>Управление фильмами</h1>
      </div>

      {/* Import row: single + bulk side by side */}
      <div className="addmovie-grid">
        {/* Single import */}
        <section className="addmovie-panel">
          <div className="addmovie-panel-head">
            <h2>Одиночный импорт</h2>
            <span>POST /movies/addFromKinopoisk</span>
          </div>
          <form onSubmit={onSingleSubmit} className="imdb-import">
            <input
              className="input"
              value={kpId}
              onChange={(e) => setKpId(e.target.value)}
              placeholder="Например: 301"
            />
            <button className="button" type="submit" disabled={addMut.isPending}>
              {addMut.isPending ? "Импорт..." : "Импортировать"}
            </button>
          </form>
          {singleMsg ? <p className="status-message">{singleMsg}</p> : null}
        </section>

        {/* Bulk import */}
        <section className="addmovie-panel">
          <div className="addmovie-panel-head">
            <h2>Массовый импорт</h2>
            <span>POST /movies/bulkImport</span>
          </div>
          <form onSubmit={onBulkSubmit} className="bulk-import-form">
            <textarea
              value={bulkText}
              onChange={(e) => { setBulkText(e.target.value); setBulkResults(null); }}
              placeholder={"301 326 405\nили по одному на строке"}
            />
            <div className="bulk-actions">
              <span className="bulk-count">{bulkIds.length} ID в очереди</span>
              <button className="button primary" type="submit" disabled={bulkMut.isPending || !bulkIds.length}>
                <Upload size={14} />
                {bulkMut.isPending ? "Импорт..." : "Импортировать список"}
              </button>
            </div>
          </form>

          {bulkResults?.error ? (
            <p className="status-message error">{bulkResults.error}</p>
          ) : null}

          {bulkResults?.results?.length ? (
            <>
              <p className="status-message" style={{ marginTop: "0.75rem" }}>
                Итог: <strong style={{ color: "#22c55e" }}>{bulkResults.success}</strong> добавлено,{" "}
                <strong style={{ color: "#ef4444" }}>{bulkResults.failed}</strong> ошибок из {bulkResults.total}
              </p>
              <div className="bulk-results">
                {bulkResults.results.map((item) => (
                  <div
                    key={item.kinopoiskId}
                    className="bulk-result"
                    style={{
                      borderColor: (STATUS_COLOR[item.status] || "#555") + "44",
                    }}
                  >
                    <span className="bulk-result-id">kp_{item.kinopoiskId}</span>
                    <span className="bulk-result-title">
                      {item.title || item.errorMessage || "—"}
                    </span>
                    <span style={{ fontSize: "0.75rem", color: STATUS_COLOR[item.status] || "#888", flexShrink: 0 }}>
                      {STATUS_LABEL[item.status] || item.status}
                    </span>
                  </div>
                ))}
              </div>
            </>
          ) : null}
        </section>
      </div>

      {/* Movie list */}
      <section className="addmovie-panel">
        <div className="admin-list-head">
          <h2>
            Список фильмов{" "}
            <span className="admin-movie-count">({filteredMovies.length} из {movies.length})</span>
          </h2>
          <input
            className="input admin-search-input"
            placeholder="Поиск по ID, названию, жанру..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        {deleteMsg ? <p className="status-message" style={{ marginBottom: "0.75rem" }}>{deleteMsg}</p> : null}

        {filteredMovies.length === 0 ? (
          <p>Фильмы не найдены.</p>
        ) : (
          <div className="movie-table-wrap">
            <table className="admin-movie-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Название</th>
                  <th>Год</th>
                  <th>Жанры</th>
                  <th>Действия</th>
                </tr>
              </thead>
              <tbody>
                {filteredMovies.map((movie) => (
                  <tr key={movie.id}>
                    <td>{movie.id}</td>
                    <td>{movie.title}</td>
                    <td>{movie.year || "—"}</td>
                    <td>{getMovieGenres(movie).join(", ") || "—"}</td>
                    <td>
                      <button
                        className="button button--danger"
                        disabled={deleteMut.isPending}
                        onClick={() => {
                          if (!window.confirm(`Удалить "${movie.title}" (ID ${movie.id})?`)) return;
                          setDeleteMsg("");
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