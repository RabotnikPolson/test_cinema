import React, { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { addFromKinopoisk, deleteMovie, useMovies } from "@/features/movies";
import "@/shared/styles/pages/AddMovie.css";

export default function AdminMoviesPage() {
  const { data: movies = [], isLoading, isError, error } = useMovies();
  const [kpId, setKpId] = useState("");
  const [msg, setMsg] = useState("");
  const qc = useQueryClient();

  const addMut = useMutation({
    mutationFn: (id) => addFromKinopoisk(id),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ["movies"] });
      setMsg(`Фильм "${data?.title || `ID: ${kpId}`}" успешно добавлен`);
      setKpId("");
    },
    onError: (err) => {
      const errorMsg =
        err.response?.data?.message || err.message || "Ошибка сервера";
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
      const backend =
        err.response?.data?.message || err.response?.data || "Ошибка сервера";
      const errorMsg = `${status ? `[${status}] ` : ""}${backend}`;
      setMsg(`Ошибка удаления: ${errorMsg}`);
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

  if (isLoading) {
    return <div className="container"><p>Загрузка списка фильмов...</p></div>;
  }

  if (isError) {
    return <div className="container"><p>Ошибка: {error?.message || "Не удалось получить фильмы"}</p></div>;
  }

  return (
    <div className="container addmovie-page">
      <h1>Админ: управление фильмами</h1>

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
                    <td>
                      <button
                        className="button button--ghost"
                        disabled={deleteMut.isPending}
                        onClick={() => {
                          if (!window.confirm(`Удалить фильм "${movie.title}" (ID ${movie.id})?`)) {
                            return;
                          }
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
