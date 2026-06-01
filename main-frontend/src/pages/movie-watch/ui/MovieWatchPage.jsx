import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { MoviePlayer } from "@/features/player";
import { CommentsSection } from "@/features/comments";
import { useAuth } from "@/features/auth";
import { useMovie } from "@/features/movies";
import {
  ReviewFormModal,
  ReviewReadModal,
  useReviewMutations,
  useReviewsByMovie,
} from "@/features/reviews";
import { ReviewCard } from "@/entities/review";
import "@/pages/movie-watch/ui/MovieWatch.css";

export default function MovieWatchPage() {
  const { t } = useTranslation();
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { data: movie, isLoading, isError, error } = useMovie(id);
  const movieId = movie?.id ?? (id ? Number(id) : null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [readOpen, setReadOpen] = useState(false);
  const [readReview, setReadReview] = useState(null);
  const [reviewMsg, setReviewMsg] = useState("");

  useEffect(() => {
    setModalOpen(false);
    setEditing(null);
    setReadOpen(false);
    setReadReview(null);
    setReviewMsg("");
  }, [id]);

  useEffect(() => {
    if (movie?.title) {
      document.title = `${movie.title} — Insight`;
    }
  }, [movie?.title]);

  const reviewsQuery = useReviewsByMovie(movieId, 0, 5);
  const reviews = reviewsQuery.data?.items || [];
  const mutations = useReviewMutations(movieId);

  const isOwnReview = (review) =>
    user?.id != null && String(review.userId) === String(user.id);
  const myReview = reviews.find(isOwnReview) || null;

  const openRead = (review) => {
    setReadReview(review);
    setReadOpen(true);
  };

  const submitReview = async ({ content, score }) => {
    setReviewMsg("");
    try {
      if (editing?.id) {
        await mutations.updateReview.mutateAsync({ id: editing.id, content, score });
      } else {
        await mutations.createReview.mutateAsync({ content, score });
      }
      setModalOpen(false);
      setEditing(null);
    } catch (reviewError) {
      console.error(reviewError);
      const status = reviewError?.response?.status;
      const serverMsg = reviewError?.response?.data?.message;
      if (status === 409) {
        setReviewMsg(t("reviews.alreadyReviewed"));
      } else if (status === 403) {
        setReviewMsg(serverMsg || t("reviews.editOnce"));
      } else {
        setReviewMsg(t("reviews.submitFailed"));
      }
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

  if (isLoading) {
    return <div className="loading container">{t("details.loadingMovie")}</div>;
  }

  if (isError) {
    return (
      <div className="container">
        <div className="error">{t("common.error")}: {error?.message || t("details.loadError")}</div>
        <button className="button button--ghost" onClick={() => navigate(-1)}>
          {t("common.back")}
        </button>
      </div>
    );
  }

  if (!movieId) {
    return (
      <div className="container">
        <div className="error">{t("details.notFound")}</div>
        <Link to="/" className="button button--ghost">
          {t("common.toHome")}
        </Link>
      </div>
    );
  }

  const title = movie.title || t("watch.movieFallback");

  return (
    <div className="watch-page">
      <div className="watch-topbar glass">
        <Link to={`/movie/${movieId}`} className="watch-back">
          ← {t("watch.backToDetails")}
        </Link>
        <span className="watch-title-inline">{title}</span>
        {myReview ? (
          !myReview.edited && (
            <button
              className="button btn-primary btn-sm"
              onClick={() => {
                setEditing(myReview);
                setReviewMsg("");
                setModalOpen(true);
              }}
            >
              {t("watch.editMyReview")}
            </button>
          )
        ) : (
          <button
            className="button btn-primary btn-sm"
            onClick={() => {
              setEditing(null);
              setReviewMsg("");
              setModalOpen(true);
            }}
          >
            {t("watch.writeReview")}
          </button>
        )}
      </div>

      <div className="watch-content">
        <MoviePlayer movie={movie} />

        <section className="watch-section glass">
          <div className="section-header">
            <h3>{t("watch.reviewsTitle")}</h3>
          </div>
          {reviewMsg ? <div className="review-inline-msg">{reviewMsg}</div> : null}
          <div className="reviews-carousel no-scrollbar">
            {reviewsQuery.isLoading ? <div className="status-text">{t("common.loading")}</div> : null}
            {reviewsQuery.isError ? <div className="status-text">{t("watch.reviewsLoadError")}</div> : null}
            {!reviewsQuery.isLoading && reviews.length === 0 ? <div className="status-text">{t("watch.noReviews")}</div> : null}
            {reviews.map((review) => (
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
            {!reviewsQuery.isLoading && reviews.length > 0 ? (
              <div className="reviews-carousel-end">
                <Link to={`/movie/${movieId}/reviews`} className="button btn-secondary">
                  {t("watch.readAllReviews")}
                </Link>
              </div>
            ) : null}
          </div>
        </section>

        <section className="watch-section glass">
          <h3>{t("watch.commentsTitle")}</h3>
          <CommentsSection movieId={movieId} />
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
