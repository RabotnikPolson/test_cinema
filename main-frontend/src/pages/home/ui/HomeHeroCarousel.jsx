import React, { useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { Navigation } from "swiper/modules";
import { Swiper, SwiperSlide } from "swiper/react";
import { useMovies } from "@/features/movies";
import { useSmartFeed } from "@/features/recommendations";
import "@/shared/styles/components/HeroCarousel.css";
import "swiper/css";
import "swiper/element/css/navigation";

export default function HomeHeroCarousel() {
  const navigate = useNavigate();
  const { data: allMovies = [], isLoading: isMoviesLoading } = useMovies();
  const { data, isLoading: isFeedLoading } = useSmartFeed();
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
            (movie) => movie.id == item.movie_id || movie.imdbId == item.movie_id
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
  }, [feed, allMovies]);

  if (isLoading) {
    return <div style={{ color: "#fff", padding: "20px" }}>Загрузка...</div>;
  }

  if (!sections.length) {
    return (
      <div className="hero-carousel hero-carousel--empty">
        <div className="container">
          <div className="carousel-header">
            <h2>Добро пожаловать в CineVerse</h2>
            <p>Здесь появятся лучшие фильмы и персональная подборка, как только база пополнится.</p>
          </div>

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

            <Swiper
              modules={[Navigation]}
              slidesPerView={5}
              spaceBetween={16}
              navigation
              breakpoints={{
                320: { slidesPerView: 1 },
                1024: { slidesPerView: 4 },
                1280: { slidesPerView: 5 },
              }}
            >
              {section.items.map((movie) => (
                <SwiperSlide
                  key={`${section.title}-${movie.id}`}
                  onClick={() => navigate(`/movie/${movie.id || movie.imdbId}`)}
                >
                  <div className="carousel-card">
                    <img
                      src={movie.posterUrl || movie.poster}
                      alt={movie.title}
                      className="carousel-poster"
                    />
                    <div className="carousel-overlay">
                      <h3>{movie.title}</h3>
                      <span>
                        {movie.year} · {movie.imdbRating || "0"}
                      </span>
                      <button className="watch-btn">Смотреть</button>
                    </div>
                  </div>
                </SwiperSlide>
              ))}
            </Swiper>
          </div>
        </div>
      ))}
    </div>
  );
}
