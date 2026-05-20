import { useState } from "react";
import { useParams } from "react-router-dom";
import { ReviewCard, useReviewsByMovie } from "@/features/reviews";

export default function MovieReviewsPage() {
  const { id } = useParams();
  const numericMovieId = Number(id);
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useReviewsByMovie(numericMovieId, page, 20);
  const items = data?.items ?? [];
  const total = data?.total ?? 0;
  const hasMore =
    typeof total === "number" ? (page + 1) * 20 < total : Boolean(data?.hasMore);

  return (
    <div style={{ maxWidth: 900, margin: "0 auto", padding: 16 }}>
      <h2>Отзывы</h2>

      {isLoading && <div>Загрузка...</div>}
      {isError && <div>Ошибка загрузки отзывов</div>}
      {!isLoading && !isError && items.length === 0 && <div>Отзывов пока нет</div>}

      <div style={{ display: "grid", gap: 12 }}>
        {items.map((review) => (
          <ReviewCard key={review.id} review={{ ...review, body: review.content }} isOwner={false} />
        ))}
      </div>

      {hasMore && (
        <div style={{ marginTop: 12, textAlign: "center" }}>
          <button onClick={() => setPage((value) => value + 1)}>Загрузить ещё</button>
        </div>
      )}
    </div>
  );
}
