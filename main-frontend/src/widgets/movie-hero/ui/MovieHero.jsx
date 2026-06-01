import React, { useRef } from "react";
import { ChevronLeft, ChevronRight, Info, Play } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Autoplay, Navigation } from "swiper/modules";
import { Swiper, SwiperSlide } from "swiper/react";
import "swiper/css";
import { useHeroMovies } from "@/features/recommendations";
import {
  formatRuntimeLabel,
  getMovieBackdrop,
  getMovieGenres,
  getMovieId,
  getMovieRating,
  getMovieSynopsis,
  getMovieTitle,
  getMovieYear,
} from "@/shared/lib/insight";
import "@/widgets/movie-hero/ui/MovieHero.css";

function HeroSlide({ movie, navigate }) {
  const { t } = useTranslation();
  const movieId = getMovieId(movie);
  const title = getMovieTitle(movie);
  const year = getMovieYear(movie);
  const ratingValue = getMovieRating(movie);
  const runtimeLabel = formatRuntimeLabel(movie);
  const genres = getMovieGenres(movie);
  const filledStars = Math.max(0, Math.min(5, Math.floor((ratingValue || 0) / 2)));

  return (
    <div
      className="hero-slide"
      style={{ backgroundImage: `url(${getMovieBackdrop(movie)})` }}
    >
      <div className="hero-banner-overlay" />
      <div className="hero-banner-gradient-left" />
      <div className="hero-banner-gradient-top" />

      <div className="hero-banner-content">
        <div className="hero-banner-label">{t("hero.trendingNow")}</div>

        <h1 className="hero-banner-title">{title}</h1>

        <p className="hero-banner-description">
          {getMovieSynopsis(movie) || t("hero.fallbackDescription")}
        </p>

        <div className="hero-banner-rating">
          {ratingValue ? (
            <>
              <span className="rating-stars">
                {"★".repeat(filledStars)}
                {"☆".repeat(5 - filledStars)}
              </span>
              <span className="rating-value">{ratingValue.toFixed(1)} / 10</span>
            </>
          ) : null}
          {year ? <span className="rating-year">{year}</span> : null}
          {runtimeLabel ? <span className="rating-chip">{runtimeLabel}</span> : null}
          {genres[0] ? <span className="rating-chip">{genres[0]}</span> : null}
        </div>

        <div className="hero-banner-actions">
          <button
            className="hero-btn hero-btn-primary"
            onClick={() => navigate(`/movie/${movieId}/watch`)}
            type="button"
          >
            <Play size={18} />
            {t("hero.watch")}
          </button>

          <button
            className="hero-btn hero-btn-secondary"
            onClick={() => navigate(`/movie/${movieId}`)}
            type="button"
          >
            <Info size={18} />
            {t("hero.details")}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function HeroBanner() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const prevRef = useRef(null);
  const nextRef = useRef(null);
  const { data: heroMovies = [] } = useHeroMovies();

  if (!heroMovies.length) return null;

  return (
    <header className="hero-banner">
      <button
        ref={prevRef}
        type="button"
        className="hero-nav-btn hero-nav-btn--prev"
        aria-label={t("home.prevMovie")}
      >
        <ChevronLeft size={22} />
      </button>

      <Swiper
        className="hero-swiper"
        modules={[Autoplay, Navigation]}
        navigation={{ prevEl: prevRef.current, nextEl: nextRef.current }}
        onBeforeInit={(swiper) => {
          swiper.params.navigation.prevEl = prevRef.current;
          swiper.params.navigation.nextEl = nextRef.current;
        }}
        autoplay={{ delay: 7000, disableOnInteraction: false, pauseOnMouseEnter: true }}
        loop={heroMovies.length > 1}
        slidesPerView={1}
        speed={700}
      >
        {heroMovies.map((movie, i) => (
          <SwiperSlide key={getMovieId(movie) ?? i}>
            <HeroSlide movie={movie} navigate={navigate} />
          </SwiperSlide>
        ))}
      </Swiper>

      <button
        ref={nextRef}
        type="button"
        className="hero-nav-btn hero-nav-btn--next"
        aria-label={t("home.nextMovie")}
      >
        <ChevronRight size={22} />
      </button>
    </header>
  );
}
