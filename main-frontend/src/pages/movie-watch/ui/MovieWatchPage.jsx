import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
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
        setReviewMsg("Вы уже оставили отзыв на этот фильм.");
      } else if (status === 403) {
        setReviewMsg(serverMsg || "Отзыв можно изменить только один раз.");
      } else {
        setReviewMsg("Не удалось отправить отзыв.");
      }
    }
  };

  const onDelete = async (reviewId) => {
    if (!reviewId) return;
    if (!window.confirm("Удалить отзыв?")) return;
    setReviewMsg("");
    try {
      await mutations.deleteReview.mutateAsync(reviewId);
    } catch (reviewError) {
      console.error(reviewError);
      setReviewMsg("Не удалось удалить отзыв.");
    }
  };

  if (isLoading) {
    return <div className="loading container">Загрузка фильма...</div>;
  }

  if (isError) {
    return (
      <div className="container">
        <div className="error">Ошибка: {error?.message || "Не удалось загрузить фильм"}</div>
        <button className="button button--ghost" onClick={() => navigate(-1)}>
          Назад
        </button>
      </div>
    );
  }

  if (!movieId) {
    return (
      <div className="container">
        <div className="error">Фильм не найден</div>
        <Link to="/" className="button button--ghost">
          На главную
        </Link>
      </div>
    );
  }

  const title = movie.title || "Фильм";

  return (
    <div className="watch-page">
      <div className="watch-topbar glass">
        <Link to={`/movie/${movieId}`} className="watch-back">
          ← Вернуться к описанию
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
              Изменить мой отзыв
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
            Написать отзыв
          </button>
        )}
      </div>

      <div className="watch-content">
        <MoviePlayer movie={movie} />

        <section className="watch-section glass">
          <div className="section-header">
            <h3>Отзывы</h3>
          </div>
          {reviewMsg ? <div className="review-inline-msg">{reviewMsg}</div> : null}
          <div className="reviews-carousel no-scrollbar">
            {reviewsQuery.isLoading ? <div className="status-text">Загрузка...</div> : null}
            {reviewsQuery.isError ? <div className="status-text">Ошибка загрузки отзывов.</div> : null}
            {!reviewsQuery.isLoading && reviews.length === 0 ? <div className="status-text">Пока нет отзывов.</div> : null}
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
                  Читать все отзывы
                </Link>
              </div>
            ) : null}
          </div>
        </section>

        <section className="watch-section glass">
          <h3>Комментарии</h3>
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
