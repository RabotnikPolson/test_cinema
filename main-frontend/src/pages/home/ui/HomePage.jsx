import React from "react";
import { useSearchParams } from "react-router-dom";
import { useAuth } from "@/features/auth";
import { useMovies, useGenres } from "@/features/movies";
import HomeHeroCarousel from "@/pages/home/ui/HomeHeroCarousel";
import { MovieGrid } from "@/shared/ui";
import "@/shared/styles/pages/Home.css";

export default function HomePage() {
  const { data: movies = [], isLoading, isError, error } = useMovies();
  const { data: genres = [] } = useGenres();
  const [searchParams] = useSearchParams();
  const q = (searchParams.get("q") || "").toLowerCase();

  const { user } = useAuth();
  const userId = user?.profile?.userId || user?.id || null;

  // Фильтры для секций
  const continueWatching = movies.filter(movie => movie.progress && movie.progress > 0).slice(0, 6);
  const newReleases = movies.filter(movie => {
    const releaseDate = new Date(movie.releaseDate);
    const monthAgo = new Date();
    monthAgo.setMonth(monthAgo.getMonth() - 1);
    return releaseDate > monthAgo;
  }).slice(0, 12);

  const editorialPicks = movies.filter(movie => movie.featured).slice(0, 6);
  const filtered = movies.filter((movie) => {
    if (!q) return true;
    return (
      movie.title.toLowerCase().includes(q) ||
      (movie.genre || "").toLowerCase().includes(q)
    );
  });

  return (
    <div className="home-page">
      {/* Hero Carousel */}
      <HomeHeroCarousel userId={userId} />

      {/* Continue Watching */}
      {continueWatching.length > 0 && (
        <section className="home-section">
          <div className="container">
            <h2 className="section-title display">Продолжить просмотр</h2>
            <MovieGrid movies={continueWatching} showProgress />
          </div>
        </section>
      )}

      {/* New Releases */}
      <section className="home-section">
        <div className="container">
          <div className="section-header">
            <h2 className="section-title display">Новинки месяца</h2>
            <a href="/movies?sort=new" className="section-link label">Все →</a>
          </div>
          <MovieGrid movies={newReleases} />
        </div>
      </section>

      {/* Editorial Picks */}
      {editorialPicks.length > 0 && (
        <section className="home-section">
          <div className="container">
            <h2 className="section-title display">Редакционный выбор</h2>
            <div className="editorial-grid">
              {editorialPicks.map(movie => (
                <div key={movie.id} className="editorial-card glass">
                  <img src={movie.posterUrl} alt={movie.title} className="editorial-image" />
                  <div className="editorial-content">
                    <h3 className="editorial-title heading">{movie.title}</h3>
                    <p className="editorial-description body">{movie.description}</p>
                    <div className="editorial-meta label">
                      <span className="badge">Фильм года</span>
                      <span>{movie.year}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* Genres */}
      <section className="home-section">
        <div className="container">
          <h2 className="section-title display">По жанрам</h2>
          <div className="genres-grid">
            {genres.slice(0, 12).map(genre => (
              <a key={genre.id} href={`/genres?filter=${genre.name}`} className="genre-card glass">
                <span className="genre-name label">{genre.name}</span>
                <span className="genre-count text-dim">{genre.movieCount} фильмов</span>
              </a>
            ))}
          </div>
        </div>
      </section>

      {/* Platform Stats */}
      <section className="home-section stats-section">
        <div className="container">
          <div className="stats-grid">
            <div className="stat-card glass">
              <div className="stat-number display">12K+</div>
              <div className="stat-label label">фильмов</div>
            </div>
            <div className="stat-card glass">
              <div className="stat-number display">4K</div>
              <div className="stat-label label">качество</div>
            </div>
            <div className="stat-card glass">
              <div className="stat-number display">50+</div>
              <div className="stat-label label">жанров</div>
            </div>
            <div className="stat-card glass">
              <div className="stat-number display">1M</div>
              <div className="stat-label label">зрителей</div>
            </div>
          </div>
        </div>
      </section>

      {/* Promo Banner - Объявление */}
      <section className="home-section promo-section">
        <div className="container">
          <div className="promo-banner glass">
            <div className="promo-content">
              <h2 className="promo-title display">Откройте премиум</h2>
              <p className="promo-description body">Без рекламы, 4K качество, неограниченный доступ</p>
              <div className="promo-plans">
                <div className="plan-card">
                  <div className="plan-name label">STANDARD</div>
                  <div className="plan-price display">790 ₸</div>
                  <div className="plan-period label">/ месяц</div>
                </div>
                <div className="plan-card featured">
                  <div className="plan-name label">PRO</div>
                  <div className="plan-price display">1490 ₸</div>
                  <div className="plan-period label">/ месяц</div>
                  <div className="badge">Популярный</div>
                </div>
              </div>
              <button className="btn btn-primary">Начать бесплатно</button>
            </div>
            <div className="promo-visual">
              <div className="promo-teaser">
                <div className="teaser-badge">Тизер</div>
                <div className="teaser-content">
                  <h3>Скоро на CineVerse</h3>
                  <p>Эксклюзивные премьеры и фестивальное кино</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Announcement Banner */}
      <section className="announcement-banner">
        <div className="container">
          <div className="announcement-content glass">
            <div className="announcement-icon"></div>
            <div className="announcement-text">
              <h3 className="announcement-title heading">Новая коллекция казахстанского кино</h3>
              <p className="announcement-description body">Откройте для себя лучшие фильмы местного производства</p>
            </div>
            <button className="btn btn-secondary">Посмотреть</button>
          </div>
        </div>
      </section>

      {/* All Movies (fallback) */}
      {q && (
        <section className="home-section">
          <div className="container">
            <h2 className="section-title display">Результаты поиска</h2>
            {isLoading && <div className="loading">Загрузка...</div>}
            {isError && <div className="error">Ошибка: {error.message}</div>}
            {!isLoading && <MovieGrid movies={filtered} />}
          </div>
        </section>
      )}
    </div>
  );
}
