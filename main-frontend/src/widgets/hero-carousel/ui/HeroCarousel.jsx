import React, { useMemo } from "react";
import { MovieCard } from "@/entities/movie";
import { useMovies } from "@/features/movies";
import { useSmartFeed } from "@/features/recommendations";
import { getMovieId } from "@/shared/lib/insight";
import { MovieCarousel } from "@/shared/ui";
import "@/widgets/hero-carousel/ui/HeroCarousel.css";

const BREAKPOINTS = {
  320: { slidesPerView: 1.1, spaceBetween: 12 },
  768: { slidesPerView: 2.4, spaceBetween: 14 },
  1024: { slidesPerView: 4, spaceBetween: 16 },
  1280: { slidesPerView: 5, spaceBetween: 16 },
};

export default function HomeHeroCarousel({ userId }) {
  const { data: allMovies = [], isLoading: isMoviesLoading } = useMovies();
  const { data, isLoading: isFeedLoading } = useSmartFeed(userId);
  const feed = data?.feed;
  const isLoading = isMoviesLoading || isFeedLoading;

  const sections = useMemo(() => {
    if (!feed || !allMovies.length) {
      return [];
    }

    const enrichMovies = (items) =>
      (items || [])
        .map((item) => {
          const fullMovie = allMovies.find(
            (movie) => movie.id == item.movie_id || movie.imdbId == item.movie_id,
          );
          return fullMovie ? { ...item, ...fullMovie } : null;
        })
        .filter(Boolean);

    const nextSections = [];

    if (feed.continue_watching?.length > 0) {
      nextSections.push({
        title: "Продолжить просмотр",
        subtitle: "Вы остановились здесь",
        items: enrichMovies(feed.continue_watching),
      });
    }

    if (feed.top_picks_for_you?.length > 0) {
      nextSections.push({
        title: "Специально для вас",
        subtitle: "AI подборка",
        items: enrichMovies(feed.top_picks_for_you),
      });
    }

    if (feed.because_you_watched?.recommendations?.length > 0) {
      nextSections.push({
        title: feed.because_you_watched.reason,
        subtitle: "Похожий контент",
        items: enrichMovies(feed.because_you_watched.recommendations),
      });
    }

    if (feed.trending?.length > 0) {
      nextSections.push({
        title: "В тренде",
        subtitle: "Популярные фильмы",
        items: enrichMovies(feed.trending),
      });
    }

    return nextSections;
  }, [allMovies, feed]);

  if (isLoading) {
    return <div style={{ color: "#fff", padding: "20px" }}>Загрузка...</div>;
  }

  if (!sections.length) {
    return (
      <div className="hero-carousel hero-carousel--empty">
        <div className="container">
          <div className="hero-placeholder-grid">
            {[1, 2, 3].map((index) => (
              <div key={index} className="hero-placeholder-card">
                <div className="hero-placeholder-image" />
                <div className="hero-placeholder-text">
                  <div className="hero-placeholder-line" />
                  <div className="hero-placeholder-line short" />
                  <div className="hero-placeholder-line" />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div>
      {sections.map((section, index) => (
        <div className="hero-carousel" key={`${section.title}-${index}`}>
          <div className="container">
            <div className="carousel-header">
              <h2>{section.title}</h2>
              <p>{section.subtitle}</p>
            </div>

            <MovieCarousel
              items={section.items}
              renderSlide={(movie) => <MovieCard movie={movie} showFavorite={false} />}
              keyExtractor={(movie, i) => `${section.title}-${getMovieId(movie) ?? i}`}
              breakpoints={BREAKPOINTS}
            />
          </div>
        </div>
      ))}
    </div>
  );
}
