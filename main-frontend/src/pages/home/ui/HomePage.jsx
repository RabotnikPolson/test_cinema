import React, { useMemo } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { ArrowRight } from "lucide-react";
import { useAuth } from "@/features/auth";
import { useGenres, useMovies } from "@/features/movies";
import { useKazakhstanRecommendations, useBecauseYouLiked, useTrending } from "@/features/recommendations";
import HeroBanner from "@/widgets/movie-hero/ui/MovieHero";
import HomeHeroCarousel from "@/widgets/hero-carousel/ui/HeroCarousel";
import { Swiper, SwiperSlide } from "swiper/react";
import { Navigation } from "swiper/modules";
import { MovieGrid } from "@/shared/ui";
import {
  formatRuntimeLabel,
  getMovieGenres,
  getMoviePoster,
  getMovieRating,
  getMovieSynopsis,
  getMovieYear,
} from "@/shared/lib/insight";
import "@/pages/home/ui/Home.css";

function mapDomesticItems(data, allMovies) {
  const items = Array.isArray(data?.recommendations)
    ? data.recommendations
    : Array.isArray(data)
      ? data
      : [];

  return items
    .map((item) => {
      const movieId = item.movie_id ?? item.id ?? item.movieId;
      const fullMovie = allMovies.find((movie) => String(movie.id) === String(movieId));
      if (fullMovie) {
        return fullMovie;
      }

      return {
        id: movieId,
        title: item.title,
        poster: item.poster_url,
        year: item.year || item.release_year,
        rating: item.rating,
        genre: item.genre,
        description: item.reason || item.description,
      };
    })
    .filter(Boolean)
    .slice(0, 3);
}

export default function HomePage() {
  const { user } = useAuth();
  const userId = user?.profile?.userId || user?.id || null;
  const { data: movies = [], isLoading, isError, error } = useMovies();
  const { data: genres = [] } = useGenres();
  const { data: domesticData } = useKazakhstanRecommendations(6);
  const { data: bYlData, isError: isBylError } = useBecauseYouLiked(user?.id);
  const { data: trendingData, isError: isTrendingError } = useTrending();
  const [searchParams] = useSearchParams();
  const q = (searchParams.get("q") || "").toLowerCase();

  const continueWatching = useMemo(
    () => movies.filter((movie) => movie.progress && movie.progress > 0).slice(0, 6),
    [movies],
  );

  const newReleases = useMemo(() => {
    return movies
      .filter((movie) => {
        const releaseDate = movie?.raw?.releaseDate || movie?.releaseDate;
        if (!releaseDate) {
          return false;
        }

        const release = new Date(releaseDate);
        const monthAgo = new Date();
        monthAgo.setMonth(monthAgo.getMonth() - 2);
        return release > monthAgo;
      })
      .slice(0, 12);
  }, [movies]);

  const featuredMovie = useMemo(() => {
    const KAZAKH_MOVIES = [
      {
        id: "peish",
        title: "Пейіш: Жұмақ ананың табанының астында",
        plot: "Пронзительная история о материнской любви, самопожертвовании и пути к прощению. Фильм, заставляющий задуматься о самых главных ценностях.",
        year: 2024,
        genre: "ДРАМА",
        posterUrl: "https://images.unsplash.com/photo-1485846234645-a62644f84728?q=80&w=2000&auto=format&fit=crop"
      },
      {
        id: "vzaperti",
        title: "Взаперти",
        plot: "Квартира становится ловушкой. Психологический триллер, где каждый шаг может стать последним, а напряжение нарастает с каждой минутой.",
        year: 2023,
        genre: "ТРИЛЛЕР, ДРАМА",
        posterUrl: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=2000&auto=format&fit=crop"
      },
      {
        id: "dastur",
        title: "Дастур",
        plot: "Девушка сталкивается с жестокими традициями и патриархальными устоями в ауле. Казахстанский хоррор, поднимающий острые социальные темы.",
        year: 2023,
        genre: "ХОРРОР, ДРАМА",
        posterUrl: "https://images.unsplash.com/photo-1509248961158-e54f6934749c?q=80&w=2000&auto=format&fit=crop"
      }
    ];
    return KAZAKH_MOVIES[Math.floor(Math.random() * KAZAKH_MOVIES.length)];
  }, []);

  const filtered = useMemo(() => {
    return movies.filter((movie) => {
      if (!q) {
        return true;
      }

      const genresText = getMovieGenres(movie).join(" ").toLowerCase();
      return movie.title.toLowerCase().includes(q) || genresText.includes(q);
    });
  }, [movies, q]);

  const highlightedDomestic = useMemo(
    () => mapDomesticItems(domesticData, movies),
    [domesticData, movies],
  );

  const domesticLead = highlightedDomestic[0];
  const domesticCards = highlightedDomestic.slice(0, 3);

  const bylItems = bYlData?.recommendations || [];
  const bylReason = bylItems.length > 0 ? (bylItems[0].reason || "Потому что вам понравился") : "Рекомендуем";
  
  const trendingItems = trendingData?.recommendations || [];

  const renderSwiper = (items) => (
    <div className="hero-carousel" style={{ paddingTop: 0, paddingBottom: 0 }}>
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
        {items.map((movie) => (
          <SwiperSlide key={movie.movie_id || movie.id}>
            <Link to={`/movie/${movie.movie_id || movie.id}`} className="carousel-card" style={{ display: 'block' }}>
              <img
                src={movie.poster_url || movie.poster}
                alt={movie.title}
                className="carousel-poster"
              />
              <div className="carousel-overlay">
                <h3>{movie.title}</h3>
                <span>
                  {movie.year} {movie.score ? `· ★${movie.score.toFixed(1)}` : ""}
                </span>
                <button className="watch-btn" type="button">Смотреть</button>
              </div>
            </Link>
          </SwiperSlide>
        ))}
      </Swiper>
    </div>
  );

  return (
    <div className="home-page">
      {featuredMovie ? <HeroBanner movie={featuredMovie} /> : null}

      <HomeHeroCarousel userId={userId} />

      {continueWatching.length > 0 ? (
        <section className="home-section">
          <div className="container">
            <div className="section-header">
              <div>
                <h2 className="section-title">Продолжить просмотр</h2>
                <p className="section-copy">Фильмы, к которым вы уже возвращались.</p>
              </div>
            </div>
            <MovieGrid movies={continueWatching} showProgress />
          </div>
        </section>
      ) : null}

      {userId && !isBylError && bylItems.length > 0 ? (
        <section className="home-section">
          <div className="container">
            <div className="section-header">
              <div>
                <h2 className="section-title">{bylReason}</h2>
                <p className="section-copy">Основано на вашей истории просмотров.</p>
              </div>
            </div>
            {renderSwiper(bylItems)}
          </div>
        </section>
      ) : null}

      {!isTrendingError && trendingItems.length > 0 ? (
        <section className="home-section">
          <div className="container">
            <div className="section-header">
              <div>
                <h2 className="section-title">В тренде</h2>
                <p className="section-copy">Самое популярное прямо сейчас.</p>
              </div>
            </div>
            {renderSwiper(trendingItems)}
          </div>
        </section>
      ) : null}

      <section className="home-section">
        <div className="container">
          <div className="section-header">
            <div>
              <h2 className="section-title">Новинки месяца</h2>
              <p className="section-copy">Свежие релизы, уже доступные в каталоге.</p>
            </div>
            {!q ? (
              <Link to="/genres" className="section-link">
                Все жанры
              </Link>
            ) : null}
          </div>

          {isLoading ? <div className="section-state">Загрузка...</div> : null}
          {isError ? <div className="section-state error">Ошибка: {error.message}</div> : null}
          {!isLoading && !isError ? <MovieGrid movies={newReleases.length ? newReleases : movies.slice(0, 12)} /> : null}
        </div>
      </section>

      {domesticLead ? (
        <section className="home-section">
          <div className="container">
            <div className="domestic-strip">
              <div className="domestic-copy">
                <div className="domestic-eyebrow">Казах кинотеатры</div>
                <h2 className="domestic-title">Казахстанское кино</h2>
                <p className="domestic-description">
                  {getMovieSynopsis(domesticLead) || "Локальные истории, фестивальные голоса и картины, которые формируют характер каталога."}
                </p>
                <div className="domestic-meta">
                  {getMovieYear(domesticLead) ? <span>{getMovieYear(domesticLead)}</span> : null}
                  {formatRuntimeLabel(domesticLead) ? <span>{formatRuntimeLabel(domesticLead)}</span> : null}
                  {getMovieRating(domesticLead) ? <span>★ {getMovieRating(domesticLead).toFixed(1)}</span> : null}
                </div>
                <Link to={`/movie/${domesticLead.id}`} className="domestic-link">
                  Открыть подборку
                  <ArrowRight size={16} />
                </Link>
              </div>

              <div className="domestic-posters">
                {domesticCards.map((movie) => (
                  <Link key={movie.id} to={`/movie/${movie.id}`} className="domestic-poster-card">
                    <img src={getMoviePoster(movie)} alt={movie.title} />
                  </Link>
                ))}
              </div>
            </div>
          </div>
        </section>
      ) : null}

      <section className="home-section">
        <div className="container">
          <div className="section-header">
            <div>
              <h2 className="section-title">По жанрам</h2>
              <p className="section-copy">Быстрый вход в подборки по настроению и стилю.</p>
            </div>
          </div>
          <div className="genres-grid">
            {genres.slice(0, 12).map((genre) => (
              <Link key={genre.id} to={`/genres?filter=${genre.name}`} className="genre-card">
                <span className="genre-name">{genre.name}</span>
                <span className="genre-count">{genre.movieCount || 0} фильмов</span>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {q ? (
        <section className="home-section">
          <div className="container">
            <div className="section-header">
              <div>
                <h2 className="section-title">Результаты поиска</h2>
                <p className="section-copy">Найдено: {filtered.length}</p>
              </div>
            </div>
            {!isLoading ? <MovieGrid movies={filtered} /> : null}
          </div>
        </section>
      ) : null}
    </div>
  );
}
