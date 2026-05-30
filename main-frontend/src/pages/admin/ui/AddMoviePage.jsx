import React, { useMemo, useState } from "react";
import { Upload } from "lucide-react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { addFromKinopoisk } from "@/features/movies";
import http from "@/shared/api/http-client";
import "@/pages/admin/ui/AddMovie.css";

function parseBulkIds(value) {
  return value
    .split(/[,\s]+/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function normalizeBulkResult(response, submittedIds) {
  const results = Array.isArray(response?.results) ? response.results : [];
  if (results.length) {
    return results.map((item) => ({
      id: item.kinopoiskId || item.id,
      ok: item.success !== false,
      title: item.title || item.movieTitle || "Без названия",
      error: item.error || item.message || "",
    }));
  }

  const success = Number(response?.success || 0);
  return submittedIds.map((id, index) => ({
    id,
    ok: index < success,
    title: index < success ? "Импортировано" : "Результат не указан",
    error: index < success ? "" : "Проверьте ответ сервера",
  }));
}

export default function AddMoviePage() {
  const [kpId, setKpId] = useState("");
  const [bulkText, setBulkText] = useState("");
  const [msg, setMsg] = useState("");
  const [bulkResults, setBulkResults] = useState([]);
  const qc = useQueryClient();

  const bulkIds = useMemo(() => parseBulkIds(bulkText), [bulkText]);

  const singleMutation = useMutation({
    mutationFn: (id) => addFromKinopoisk(id),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ["movies"] });
      setMsg(`Фильм "${data?.title || `ID: ${kpId}`}" успешно добавлен.`);
      setKpId("");
    },
    onError: (err) => {
      const errorMsg =
        err.response?.data?.message || err.message || "Ошибка сервера";
      setMsg(`Ошибка: ${errorMsg}`);
    },
  });

  const bulkMutation = useMutation({
    mutationFn: async (ids) => {
      const response = await http.post("/movies/bulkImport", { kinopoiskIds: ids });
      return response.data;
    },
    onSuccess: (data, ids) => {
      qc.invalidateQueries({ queryKey: ["movies"] });
      setBulkResults(normalizeBulkResult(data, ids));
      setMsg(`Массовый импорт завершён: ${data?.success ?? ids.length} из ${data?.total ?? ids.length}.`);
    },
    onError: (err) => {
      const errorMsg =
        err.response?.data?.message || err.message || "Не удалось выполнить массовый импорт";
      setMsg(`Ошибка bulk-import: ${errorMsg}`);
      setBulkResults([]);
    },
  });

  const submitSingle = (event) => {
    event.preventDefault();
    const id = kpId.trim();

    if (!/^\d+$/.test(id)) {
      setMsg("Введите корректный числовой Kinopoisk ID.");
      return;
    }

    singleMutation.mutate(id);
  };

  const submitBulk = (event) => {
    event.preventDefault();

    if (!bulkIds.length) {
      setMsg("Добавьте хотя бы один Kinopoisk ID для массового импорта.");
      return;
    }

    const invalid = bulkIds.find((id) => !/^\d+$/.test(id));
    if (invalid) {
      setMsg(`Некорректный ID в списке: ${invalid}`);
      return;
    }

    bulkMutation.mutate(bulkIds);
  };

  return (
    <div className="addmovie-shell">
      <div className="addmovie-hero">
        <div>
          <div className="addmovie-eyebrow">Admin import</div>
          <h1>Импорт фильмов</h1>
          <p>Сохранён текущий импорт по одному ID и добавлен пакетный сценарий поверх основного API.</p>
        </div>
      </div>

      <div className="addmovie-grid">
        <section className="addmovie-panel">
          <div className="addmovie-panel-head">
            <div>
              <h2>Одиночный импорт</h2>
              <span>POST /movies/addFromKinopoisk</span>
            </div>
          </div>

          <form onSubmit={submitSingle} className="imdb-import">
            <input
              className="input"
              value={kpId}
              onChange={(event) => setKpId(event.target.value)}
              placeholder="Например: 301"
            />
            <button className="button" disabled={singleMutation.isPending} type="submit">
              {singleMutation.isPending ? "Импорт..." : "Импортировать"}
            </button>
          </form>
        </section>

        <section className="addmovie-panel addmovie-panel-wide">
          <div className="addmovie-panel-head">
            <div>
              <h2>Массовый импорт</h2>
              <span>POST /movies/bulkImport</span>
            </div>
          </div>

          <form onSubmit={submitBulk} className="bulk-import-form">
            <textarea
              value={bulkText}
              onChange={(event) => setBulkText(event.target.value)}
              placeholder={"301\n326\n405"}
            />
            <div className="bulk-actions">
              <div className="bulk-count">{bulkIds.length} ID в очереди</div>
              <button className="button primary" disabled={bulkMutation.isPending} type="submit">
                <Upload size={14} />
                {bulkMutation.isPending ? "Импорт..." : "Импортировать список"}
              </button>
            </div>
          </form>

          {bulkResults.length ? (
            <div className="bulk-results">
              {bulkResults.map((item) => (
                <div key={`${item.id}-${item.title}`} className={`bulk-result ${item.ok ? "ok" : "err"}`}>
                  <span className="bulk-result-id">kp_{item.id}</span>
                  <span className="bulk-result-title">{item.ok ? item.title : item.error}</span>
                </div>
              ))}
            </div>
          ) : null}
        </section>
      </div>

      {msg ? <p className="status-message">{msg}</p> : null}
    </div>
  );
}
