import { useEffect, useMemo, useState } from "react";
import "@/shared/styles/components/reviewModal.css";

function StarRating({ value, onChange }) {
  const [hover, setHover] = useState(0);

  return (
    <div className="review-rating" role="radiogroup" aria-label="Оценка">
      {Array.from({ length: 10 }).map((_, i) => {
        const n = i + 1;
        const active = n <= (hover || value);

        return (
          <button
            key={n}
            type="button"
            className={`review-star ${active ? "is-active" : ""}`}
            onMouseEnter={() => setHover(n)}
            onMouseLeave={() => setHover(0)}
            onClick={() => onChange(n)}
            aria-label={`Оценка ${n} из 10`}
          >
            ★
          </button>
        );
      })}

      <div className="review-rating-value">
        {value ? `${value}/10` : "—/10"}
      </div>
    </div>
  );
}

export default function ReviewFormModal({ open, initial, onClose, onSubmit }) {
  const [content, setContent] = useState("");
  const [score, setScore] = useState(0);
  const [err, setErr] = useState("");

  const isEdit = useMemo(() => Boolean(initial?.id), [initial]);

  useEffect(() => {
    if (!open) return;
    setContent(initial?.content ?? "");
    setScore(initial?.score ?? 0);
    setErr("");
  }, [open, initial]);

  useEffect(() => {
    if (!open) return;

    const onKeyDown = (e) => {
      if (e.key === "Escape") onClose?.();
    };

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  const handleSubmit = async () => {
    const trimmed = content.trim();

    if (!trimmed) {
      setErr("Текст отзыва не должен быть пустым.");
      return;
    }
    if (!score || score < 1 || score > 10) {
      setErr("Поставь оценку от 1 до 10.");
      return;
    }

    setErr("");
    await onSubmit({ content: trimmed, score });
  };

  return (
    <div className="review-modal-overlay" onMouseDown={onClose}>
      <div className="review-modal-card" onMouseDown={(e) => e.stopPropagation()}>
        <div className="review-modal-head">
          <h3 className="review-modal-title">
            {isEdit ? "Изменить отзыв" : "Написать отзыв"}
          </h3>

          <button className="review-modal-close" type="button" onClick={onClose}>
            ✕
          </button>
        </div>

        <div className="review-modal-block">
          <div className="review-modal-label">Оценка</div>
          <StarRating value={score} onChange={setScore} />
        </div>

        <div className="review-modal-block">
          <div className="review-modal-label">Текст</div>
          <textarea
            className="review-modal-textarea"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            rows={6}
            placeholder="Напиши, что понравилось/не понравилось..."
          />
        </div>

        {err && <div className="review-modal-error">{err}</div>}

        <div className="review-modal-actions">
          <button className="review-btn-primary" type="button" onClick={handleSubmit}>
            Отправить
          </button>
          <button className="review-btn-ghost" type="button" onClick={onClose}>
            Отмена
          </button>
        </div>
      </div>
    </div>
  );
}
