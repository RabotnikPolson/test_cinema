import React, { useState } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useReplies } from "@/features/reviews";
import "./ReviewCard.css";

export default function ReviewCard({ review, onReadFull, isOwner, onEdit, onDelete }) {
  const { t } = useTranslation();
  const date = review.createdAt ? new Date(review.createdAt).toLocaleDateString() : "";
  const content = review.content || "";
  const short = content.length > 200 ? content.slice(0, 200).trim() + "…" : content;

  const [showReplies, setShowReplies] = useState(false);
  const [replyPage, setReplyPage] = useState(0);

  const { data: repliesData, isLoading: repliesLoading } = useReplies(review.id, replyPage, 5, { enabled: showReplies });

  const replies = repliesData?.items || [];
  const hasMoreReplies = (replyPage + 1) * 5 < (repliesData?.total || 0);

  return (
    <div className="review-card glass">
      <div className="review-card-content">
        <div className="review-card-header">
          <div className="review-author-info">
            <div className="review-avatar">
              {review.authorAvatarUrl ? (
                <img src={review.authorAvatarUrl} alt={review.authorUsername} />
              ) : (
                <span className="review-avatar-fallback">
                  {review.authorUsername?.[0]?.toUpperCase() || "👤"}
                </span>
              )}
            </div>
            <div className="review-meta">
              <Link to={`/activity/${review.userId}`} state={{ username: review.authorUsername }} className="review-author-link">
                <span className="review-author-name">
                  {review.authorUsername || t("reviews.userFallback")}
                </span>
              </Link>
              <span className="review-date">
                {date}
                {review.edited && <span className="review-edited"> · {t("reviews.edited")}</span>}
              </span>
            </div>
          </div>

          {typeof review.score === "number" && (
            <div className={`review-score ${review.score >= 7 ? "score-high" : review.score >= 5 ? "score-medium" : "score-low"}`}>
              {review.score}
            </div>
          )}
        </div>

        <div className="review-text">
          {short}
        </div>

        <div className="review-actions">
          {content.length > 200 && (
            <button className="review-btn btn-read-more" onClick={() => onReadFull?.(review)}>
              {t("reviews.readFull")}
            </button>
          )}

          {isOwner && (
            <div className="review-owner-actions">
              {!review.edited && (
                <button className="review-btn btn-edit" onClick={onEdit}>{t("common.edit")}</button>
              )}
              <button className="review-btn btn-delete" onClick={onDelete}>{t("common.delete")}</button>
            </div>
          )}

          {review.replyCount > 0 && (
             <button className="review-btn btn-replies" onClick={() => setShowReplies(!showReplies)}>
               {showReplies ? t("reviews.hideReplies") : t("reviews.showReplies", { count: review.replyCount })}
             </button>
          )}
        </div>

        {showReplies && (
          <div className="review-replies" style={{ marginTop: '1rem', paddingLeft: '1rem', borderLeft: '2px solid rgba(255,255,255,0.1)' }}>
             {repliesLoading && <div className="loading" style={{ fontSize: '0.9rem', color: '#999' }}>{t("reviews.loadingReplies")}</div>}
             <div style={{ display: "grid", gap: 8, marginTop: 8 }}>
               {replies.map(reply => (
                 <ReviewCard key={reply.id} review={{ ...reply, body: reply.content }} isOwner={false} />
               ))}
             </div>
             {hasMoreReplies && (
               <button onClick={() => setReplyPage(p => p + 1)} className="review-btn" style={{ marginTop: '1rem' }}>{t("reviews.loadMoreReplies")}</button>
             )}
          </div>
        )}
      </div>
    </div>
  );
}
