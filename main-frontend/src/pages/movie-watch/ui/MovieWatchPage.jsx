import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { CommentsSection } from "@/features/comments";
import { getStream, useMovie } from "@/features/movies";
import WatchRecommendationsRail from "@/features/recommendations/ui/RightRailTabs";
import {
  ReviewCard,
  ReviewFormModal,
  ReviewReadModal,
  useReviewMutations,
  useReviewsByMovie,
} from "@/features/reviews";
import { WatchTracker } from "@/features/watch-history";
import "@/shared/styles/pages/MovieWatch.css";

function MoviePlayer({ streamData, isLoading, isError, onRetry, movieId }) {
  return (
    <div className="watch-player glass">
      <div className="watch-player-top">
        <span>Видео</span>
        <button className="button button--ghost" onClick={onRetry} disabled={isLoading}>
          Обновить источник
        </button>
      </div>

      <div className="watch-player-stage">
        {isLoading && <div className="watch-player-status">Загрузка видео...</div>}
        {isError && (
          <div className="watch-player-status watch-player-status-error">
            Не удалось загрузить видео.
            <button className="button button--ghost" onClick={onRetry}>
              Повторить
            </button>
          </div>
        )}

        {!isLoading && !isError && streamData?.url ? (
          <div className="watch-video-card">
            <WatchTracker url={streamData.url} movieId={movieId} />
          </div>
        ) : (
          !isLoading && (
            <div className="watch-player-fallback">
              Источник недоступен. Попробуйте открыть в новой вкладке.
            </div>
          )
        )}
      </div>

      <div className="watch-player-bottom">
        <button
          className="button btn-primary"
          onClick={() => window.open(streamData?.url, "_blank", "noopener,noreferrer")}
          disabled={!streamData?.url || isLoading}
        >
          Открыть в новой вкладке
        </button>
        {streamData?.quality && <span className="watch-quality">{streamData.quality}</span>}
      </div>
    </div>
  );
}

export default function MovieWatchPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data: movie, isLoading, isError, error } = useMovie(id);
  const movieId = movie?.id ?? (id ? Number(id) : null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [readOpen, setReadOpen] = useState(false);
  const [readReview, setReadReview] = useState(null);

  useEffect(() => {
    setModalOpen(false);
    setEditing(null);
    setReadOpen(false);
    setReadReview(null);
  }, [id]);

  useEffect(() => {
    if (movie?.title) {
      document.title = `${movie.title} — CineVerse`;
    }
  }, [movie?.title]);

  const streamQuery = useQuery({
    queryKey: ["stream", movieId],
    queryFn: () => getStream(movieId),
    enabled: !!movieId,
  });

  const reviewsQuery = useReviewsByMovie(movieId, 0, 5);
  const reviews = reviewsQuery.data?.items || [];
  const totalReviews = reviewsQuery.data?.total ?? 0;
  const mutations = useReviewMutations(movieId);

  const openRead = (review) => {
    setReadReview(review);
    setReadOpen(true);
  };

  const submitReview = async ({ content, score }) => {
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
      alert("Не удалось отправить отзыв.");
    }
  };

  const onDelete = async (reviewId) => {
    if (!reviewId) {
      return;
    }
    if (!window.confirm("Удалить отзыв?")) {
      return;
    }
    try {
      await mutations.deleteReview.mutateAsync(reviewId);
    } catch (reviewError) {
      console.error(reviewError);
      alert("Не удалось удалить отзыв.");
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
  const meta = [
    movie.year && `Год: ${movie.year}`,
    movie.runtime && `Длительность: ${movie.runtime} мин`,
    movie.genre && `Жанр: ${movie.genre}`,
    movie.imdbRating && `IMDb: ${movie.imdbRating}`,
  ]
    .filter(Boolean)
    .join(" · ");

  return (
    <div className="watch-page">
      <div className="watch-topbar glass">
        <Link to={`/movie/${movieId}`} className="watch-back">
          Вернуться к описанию
        </Link>
        <span className="watch-badge">Смотреть</span>
      </div>

      <div className="watch-grid">
        <main className="watch-main">
          <MoviePlayer
            streamData={streamQuery.data}
            isLoading={streamQuery.isLoading}
            isError={streamQuery.isError}
            onRetry={() => streamQuery.refetch()}
            movieId={movieId}
          />

          <div className="watch-headline glass">
            <div>
              <h1>{title}</h1>
              <p>{meta}</p>
            </div>
            <button className="button btn-primary" onClick={() => setModalOpen(true)}>
              Написать отзыв
            </button>
          </div>

          <section className="watch-section glass">
            <div className="section-header">
              <h3>Описание</h3>
            </div>
            <p>{movie.description || "Описание отсутствует."}</p>
          </section>

          <section className="watch-section glass">
            <div className="section-header">
              <h3>Отзывы</h3>
            </div>
            <div className="reviews-carousel no-scrollbar">
              {reviewsQuery.isLoading && <div className="status-text">Загрузка...</div>}
              {reviewsQuery.isError && <div className="status-text">Ошибка загрузки отзывов.</div>}
              {!reviewsQuery.isLoading && reviews.length === 0 && <div className="status-text">Пока нет отзывов.</div>}
              {reviews.map((review) => (
                <ReviewCard
                  key={review.id}
                  review={review}
                  moviePoster={movie.poster || movie.posterUrl}
                  onReadFull={openRead}
                  isOwner={false}
                  onEdit={() => {
                    setEditing(review);
                    setModalOpen(true);
                  }}
                  onDelete={() => onDelete(review.id)}
                />
              ))}
              {!reviewsQuery.isLoading && reviews.length > 0 && (
                <div className="reviews-carousel-end">
                  <Link to={`/movie/${movieId}/reviews`} className="button btn-secondary">
                    Читать все отзывы
                  </Link>
                </div>
              )}
            </div>
          </section>

          <section className="watch-section glass">
            <h3>Комментарии</h3>
            <CommentsSection movieId={movieId} />
          </section>
        </main>

        <aside className="watch-sidebar">
          <WatchRecommendationsRail movieId={movieId} />
        </aside>
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
