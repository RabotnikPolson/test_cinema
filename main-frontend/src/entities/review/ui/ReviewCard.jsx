import React, { useState } from "react";
import { useReplies } from "@/features/reviews";
import "./ReviewCard.css";

export default function ReviewCard({ review, moviePoster, onReadFull, isOwner, onEdit, onDelete }) {
  const date = review.createdAt ? new Date(review.createdAt).toLocaleDateString() : "";
  const content = review.content || "";
  const short = content.length > 200 ? content.slice(0, 200).trim() + "…" : content;
  
  // Dynamic poster logic if not passed down
  const bgImage = moviePoster || review.moviePosterUrl || "https://placehold.jp/12121c/12121c/300x450.png?text=INSIGHT";

  const [showReplies, setShowReplies] = useState(false);
  const [replyPage, setReplyPage] = useState(0);

  const { data: repliesData, isLoading: repliesLoading } = useReplies(review.id, replyPage, 5, { enabled: showReplies });

  const replies = repliesData?.items || [];
  const hasMoreReplies = (replyPage + 1) * 5 < (repliesData?.total || 0);

  return (
    <div className="review-card glass">
      <div className="review-card-bg">
        <img src={bgImage} alt="Movie Poster Background" loading="lazy" />
        <div className="review-card-overlay"></div>
      </div>
      
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
              <span className="review-author-name">
                {review.authorUsername || "Пользователь"}
              </span>
              <span className="review-date">{date}</span>
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
              Читать полностью
            </button>
          )}
          
          {isOwner && (
            <div className="review-owner-actions">
              <button className="review-btn btn-edit" onClick={onEdit}>Изменить</button>
              <button className="review-btn btn-delete" onClick={onDelete}>Удалить</button>
            </div>
          )}

          {review.replyCount > 0 && (
             <button className="review-btn btn-replies" onClick={() => setShowReplies(!showReplies)}>
               {showReplies ? "Скрыть ответы" : `Показать ответы (${review.replyCount})`}
             </button>
          )}
        </div>
        
        {showReplies && (
          <div className="review-replies" style={{ marginTop: '1rem', paddingLeft: '1rem', borderLeft: '2px solid rgba(255,255,255,0.1)' }}>
             {repliesLoading && <div className="loading" style={{ fontSize: '0.9rem', color: '#999' }}>Загрузка ответов...</div>}
             <div style={{ display: "grid", gap: 8, marginTop: 8 }}>
               {replies.map(reply => (
                 <ReviewCard key={reply.id} review={{ ...reply, body: reply.content }} moviePoster={bgImage} isOwner={false} />
               ))}
             </div>
             {hasMoreReplies && (
               <button onClick={() => setReplyPage(p => p + 1)} className="review-btn" style={{ marginTop: '1rem' }}>Загрузить еще ответы</button>
             )}
          </div>
        )}
      </div>
    </div>
  );
}
