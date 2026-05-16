import React, { useState } from "react";
import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { CommentItem } from "@/features/comments";
import { ReviewCard } from "@/features/reviews";
import http from "@/shared/api/http-client";
import "@/shared/styles/pages/Profile.css";

export default function UserActivityPage() {
  const { username } = useParams();
  const [editingReview, setEditingReview] = useState(null);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["userActivity", username],
    queryFn: async () => {
      const [reviewsRes, commentsRes] = await Promise.all([
        http.get(`/reviews/user/${username}`),
        http.get(`/comments/user/${username}`),
      ]);

      const reviews = (reviewsRes.data || []).map((item) => ({ ...item, type: "review" }));
      const comments = (commentsRes.data || []).map((item) => ({ ...item, type: "comment" }));
      const combined = [...reviews, ...comments];

      combined.sort(
        (left, right) => new Date(right.ts || right.createdAt) - new Date(left.ts || left.createdAt)
      );

      return combined;
    },
    retry: 1,
  });

  const openReview = (review) => setEditingReview(review);

  if (isLoading) {
    return <p className="profile-muted">Загрузка...</p>;
  }

  if (isError) {
    return <p className="profile-muted">Не удалось загрузить активность.</p>;
  }

  if (!data || data.length === 0) {
    return <p className="profile-muted">Нет активности.</p>;
  }

  return (
    <div className="container profile-page">
      <div className="profile-shell">
        <h1 className="profile-title">Активность пользователя {username}</h1>

        <div className="profile-card">
          {data.map((item, index) => {
            if (item.type === "review") {
              return (
                <ReviewCard
                  key={`review-${item.id || index}`}
                  review={item}
                  onReadFull={openReview}
                  isOwner
                  onEdit={() => setEditingReview(item)}
                  onDelete={() => alert(`Удалить отзыв ${item.id}`)}
                />
              );
            }

            if (item.type === "comment") {
              return (
                <CommentItem
                  key={`comment-${item.id || index}`}
                  node={item}
                  isReply={false}
                  onReply={(text) => alert(`Ответ ${item.id}: ${text}`)}
                  onDelete={() => alert(`Удалить комментарий ${item.id}`)}
                  onReact={(emoji) => alert(`Реакция ${emoji} на ${item.id}`)}
                />
              );
            }

            return null;
          })}
        </div>
      </div>
    </div>
  );
}
