import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuth } from "@/features/auth";
import { ReviewCard } from "@/entities/review";
import {
  ReviewFormModal,
  ReviewReadModal,
  useReviewMutations,
  useReviewsByMovie,
} from "@/features/reviews";
import "@/pages/movie-watch/ui/MovieWatch.css";
import "./MovieReviews.css";

const PAGE_SIZE = 20;

export default function MovieReviewsPage() {
  const { t } = useTranslation();
  const { id } = useParams();
  const { user } = useAuth();
  const numericMovieId = Number(id);
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [readOpen, setReadOpen] = useState(false);
  const [readReview, setReadReview] = useState(null);
  const [reviewMsg, setReviewMsg] = useState("");

  const { data, isLoading, isError } = useReviewsByMovie(numericMovieId, page, PAGE_SIZE);
  const mutations = useReviewMutations(numericMovieId);

  const items = data?.items ?? [];
  const total = data?.total ?? 0;
  const hasMore = (page + 1) * PAGE_SIZE < total;

  useEffect(() => {
    setReviewMsg("");
  }, [id]);

  const isOwnReview = (review) =>
    user?.id != null && String(review.userId) === String(user.id);

  const openRead = (review) => {
    setReadReview(review);
    setReadOpen(true);
  };

  const submitReview = async ({ content, score }) => {
    setReviewMsg("");
    try {
      if (editing?.id) {
        await mutations.updateReview.mutateAsync({ id: editing.id, content, score });
      }
      setModalOpen(false);
      setEditing(null);
    } catch (reviewError) {
      console.error(reviewError);
      const status = reviewError?.response?.status;
      const serverMsg = reviewError?.response?.data?.message;
      setReviewMsg(status === 403 ? (serverMsg || t("reviews.editOnce")) : t("reviews.saveFailed"));
    }
  };

  const onDelete = async (reviewId) => {
    if (!reviewId) return;
    if (!window.confirm(t("reviews.deleteConfirm"))) return;
    setReviewMsg("");
    try {
      await mutations.deleteReview.mutateAsync(reviewId);
    } catch (reviewError) {
      console.error(reviewError);
      setReviewMsg(t("reviews.deleteFailed"));
    }
  };

  return (
    <div className="watch-page">
      <div className="watch-topbar glass">
        <Link to={`/movie/${numericMovieId}/watch`} className="watch-back">
          ← {t("reviews.backToWatch")}
        </Link>
        <span className="watch-title-inline">{t("reviews.allReviews")}</span>
      </div>

      <div className="watch-content">
        <section className="watch-section glass">
          <div className="section-header">
            <h3>{t("watch.reviewsTitle")} {total ? `(${total})` : ""}</h3>
          </div>

          {reviewMsg ? <div className="review-inline-msg">{reviewMsg}</div> : null}

          {isLoading && <div className="status-text">{t("common.loading")}</div>}
          {isError && <div className="status-text">{t("watch.reviewsLoadError")}</div>}
          {!isLoading && !isError && items.length === 0 && (
            <div className="status-text">{t("reviews.noneYet")}</div>
          )}

          <div className="reviews-grid">
            {items.map((review) => (
              <ReviewCard
                key={review.id}
                review={review}
                onReadFull={openRead}
                isOwner={isOwnReview(review)}
                onEdit={() => {
                  setEditing(review);
                  setReviewMsg("");
                  setModalOpen(true);
                }}
                onDelete={() => onDelete(review.id)}
              />
            ))}
          </div>

          {hasMore && (
            <div className="reviews-grid-more">
              <button className="button btn-secondary" onClick={() => setPage((value) => value + 1)}>
                {t("common.loadMore")}
              </button>
            </div>
          )}
        </section>
      </div>

      <ReviewFormModal
        open={modalOpen}
        initial={editing}
        onClose={() => {
          setModalOpen(false);
          setEditing(null);
        }}
        onSubmit={submitReview}
      />

      <ReviewReadModal
        open={readOpen}
        review={readReview}
        onClose={() => {
          setReadOpen(false);
          setReadReview(null);
        }}
      />
    </div>
  );
}
