import React, { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { addFromKinopoisk } from "@/features/movies";
import "@/shared/styles/pages/AddMovie.css";

export default function AddMoviePage() {
  const [kpId, setKpId] = useState("");
  const [msg, setMsg] = useState("");
  const qc = useQueryClient();

  const mutation = useMutation({
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

  const submit = (event) => {
    event.preventDefault();
    const id = kpId.trim();

    if (!/^\d+$/.test(id)) {
      setMsg("Введите корректный числовой ID");
      return;
    }

    mutation.mutate(id);
  };

  return (
    <div className="addmovie-page container">
      <h1>Добавить фильм по ID Кинопоиска</h1>

      <form onSubmit={submit} className="imdb-import">
        <input
          className="input"
          value={kpId}
          onChange={(event) => setKpId(event.target.value)}
          placeholder="Например: 301"
        />
        <button className="button" disabled={mutation.isPending}>
          {mutation.isPending ? "Импорт..." : "Импортировать"}
        </button>
      </form>

      {msg && (
        <p className={`status-message ${mutation.isError ? "error" : "success"}`} role="status">
          {msg}
        </p>
      )}
    </div>
  );
}
