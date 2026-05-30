import React, { useMemo } from 'react';
import { CheckCircle2 } from 'lucide-react';
import { getSavedMovieProgressPercent } from '@/shared/utils';
import { getMoviePoster, getMovieTitle, getMovieYear } from '@/shared/lib/insight';
import { useParams, useLocation, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/features/auth';
import http from '@/shared/api/http-client';
import { useMovies } from '@/features/movies';
import { useBecauseYouLiked } from '@/features/recommendations';
import { MovieGrid } from '@/shared/ui';
import { getMovieId } from '@/shared/lib/insight';
import { useHistoryStorage } from '@/shared/utils/localHistory';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';
import { Bar } from 'react-chartjs-2';
import '@/pages/user-profile/ui/Profile.css';

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend
);

/* ── helpers: enrich rec IDs with full movie data from catalog ── */

function buildMovieLookup(movies) {
  const lookup = new Map();
  movies.forEach((m) => {
    if (m?.id != null) lookup.set(String(m.id), m);
    if (m?.imdbId) lookup.set(String(m.imdbId), m);
  });
  return lookup;
}

function enrichAndDedupe(items, lookup, limit = 10) {
  const seen = new Set();
  return (items || [])
    .map((item) => {
      const id = getMovieId(item);
      const catalog = id != null ? lookup.get(String(id)) : null;
      return catalog
        ? { ...item, ...catalog, movie_id: item.movie_id ?? catalog.id }
        : item;
    })
    .filter((item) => {
      const id = getMovieId(item);
      if (id == null || seen.has(String(id))) return false;
      seen.add(String(id));
      return true;
    })
    .slice(0, limit);
}

/* ── component ── */

export default function UserActivityPage() {
  const { username: userId } = useParams();
  const location = useLocation();
  const { user: currentUser } = useAuth();
  const localHistory = useHistoryStorage();

  const passedUsername = location.state?.username;
  const isOwner = currentUser && String(currentUser.id) === String(userId);
  const userEmail = isOwner ? currentUser.email : passedUsername;

  // catalog for enriching IDs → full movie objects
  const { data: movies = [] } = useMovies();
  const movieLookup = useMemo(() => buildMovieLookup(movies), [movies]);

  // recent history from localStorage (only for owner)
  const historyMovies = useMemo(() => {
    if (!isOwner) return [];
    return enrichAndDedupe(localHistory.read(), movieLookup, 10);
  }, [isOwner, movieLookup]);

  /* ─── 1. Profile (display name) ─── */
  const { data: profile } = useQuery({
    queryKey: ['activityProfile', userEmail],
    queryFn: async () => {
      if (!userEmail) return null;
      const res = await http.get(isOwner ? '/profile/me' : `/profile/${userEmail}`);
      return res.data;
    },
    enabled: !!userEmail,
  });

  /* ─── 2. Analytics — hours & genres (owner only) ─── */
  const { data: analytics, isLoading: analyticsLoading } = useQuery({
    queryKey: ['activityAnalytics', userId],
    queryFn: async () => {
      const res = await http.get('/analytics/me/summary');
      return res.data;
    },
    enabled: isOwner,
  });

  /* ─── 3. Watchlist (owner only, enriched via movieLookup) ─── */
  const { data: rawWatchlist, isLoading: watchlistLoading } = useQuery({
    queryKey: ['activityWatchlist', userId],
    queryFn: async () => {
      const res = await http.get(`/watchlists/user/${userId}`);
      return res.data;
    },
    enabled: isOwner,
  });

  const watchlistMovies = useMemo(() => {
    if (!rawWatchlist?.length) return [];
    return rawWatchlist
      .map((w) => movieLookup.get(String(w.movieId)))
      .filter(Boolean);
  }, [rawWatchlist, movieLookup]);

  /* ─── 4. Recommendations (AI) ───
   * TODO: switch to Java-прокси GET /api/recommendations/because-you-liked
   *       when the endpoint is added to Java backend.
   *       Right now useBecauseYouLiked calls Python rec_ai directly via aiHttp. */
  const { data: recData, isLoading: recLoading } = useBecauseYouLiked(userId);
  const personalizedItems = useMemo(
    () => enrichAndDedupe(recData?.recommendations, movieLookup, 10),
    [recData, movieLookup],
  );

  /* ─── 5. Ratings + Reviews via GET /profile/{username}/ratings ─── */
  const { data: ratingsData, isLoading: ratingsLoading } = useQuery({
    queryKey: ['activityRatings', userEmail],
    queryFn: async () => {
      const res = await http.get(`/profile/${userEmail}/ratings?page=0&size=50`);
      return res.data;
    },
    enabled: !!userEmail,
  });

  const ratings = ratingsData?.ratings || [];

  /* ─── derived ─── */
  const displayName = profile?.username || profile?.displayName || userEmail || `#${userId}`;
  const totalHours = Math.round((analytics?.totalSeconds || 0) / 3600);

  const chartData = useMemo(() => {
    if (!analytics?.activityByDay) return null;
    return {
      labels: analytics.activityByDay.map(p => p.day),
      datasets: [
        {
          label: 'Секунд за день',
          data: analytics.activityByDay.map(p => p.seconds),
          backgroundColor: 'rgba(201, 168, 76, 0.7)',
          borderColor: '#C9A84C',
          borderWidth: 1,
          borderRadius: 4,
        },
      ],
    };
  }, [analytics]);

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      title: { display: false },
    },
    scales: {
      y: { beginAtZero: true, grid: { color: 'rgba(255,255,255,0.1)' }, ticks: { color: '#888' } },
      x: { grid: { display: false }, ticks: { color: '#888' } },
    },
  };

  /* ─── render ─── */
  return (
    <div className="profile-page">
      <div className="profile-shell">
        <h1 className="profile-title">Активность {displayName}</h1>

        <div style={{display: 'flex', flexDirection: 'column', gap: '24px', width: '100%'}}>

          {/* ═══ АНАЛИТИКА ═══ */}
          <section className="profile-card glass">
            <h2 className="profile-section-title">📊 Аналитика просмотров</h2>
            {isOwner ? (
                analyticsLoading ? (
                    <p className="profile-muted">Загрузка аналитики…</p>
                ) : (
                    <div style={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px'}}>
                      <div style={{padding: '16px', background: 'rgba(255,255,255,0.05)', borderRadius: '8px'}}>
                        <p style={{fontSize: '13px', color: '#888', margin: 0}}>Всего посмотрено</p>
                        <p style={{fontSize: '28px', fontWeight: 700, color: '#C9A84C', margin: '4px 0 0'}}>
                          {totalHours} ч
                        </p>
                        <div style={{marginTop: '16px', height: '150px'}}>
                          {chartData && chartData.labels.length > 0 ? (
                              <Bar options={chartOptions} data={chartData}/>
                          ) : (
                              <p style={{color: '#666', fontSize: '13px', textAlign: 'center', marginTop: '40px'}}>Нет
                                активности по дням</p>
                          )}
                        </div>
                      </div>
                      <div style={{padding: '16px', background: 'rgba(255,255,255,0.05)', borderRadius: '8px'}}>
                        <p style={{fontSize: '13px', color: '#888', margin: 0}}>Любимые жанры</p>
                        {analytics?.genresPie?.length ? (
                            <ul style={{
                              paddingLeft: '18px',
                              margin: '6px 0 0',
                              color: '#E0E0E0',
                              fontSize: '14px',
                              display: 'flex',
                              flexDirection: 'column',
                              gap: '8px'
                            }}>
                              {analytics.genresPie.slice(0, 5).map((g, i) => {
                                const hours = Math.round(g.value / 3600);
                                const percentage = Math.round((g.value / (analytics.totalSeconds || 1)) * 100);
                                return (
                                    <li key={i} style={{
                                      display: 'flex',
                                      justifyContent: 'space-between',
                                      alignItems: 'center'
                                    }}>
                                      <span>{g.label}</span>
                                      <span style={{color: '#C9A84C', fontWeight: 'bold'}}>{hours} ч <span
                                          style={{color: '#666', fontSize: '12px'}}>({percentage}%)</span></span>
                                    </li>
                                );
                              })}
                            </ul>
                        ) : (
                            <p style={{color: '#666', margin: '6px 0 0', fontSize: '14px'}}>Нет данных</p>
                        )}
                      </div>
                    </div>
                )
            ) : (
                <p className="profile-muted">Аналитика доступна только владельцу профиля.</p>
            )}
          </section>


          {/* ═══ ПОСЛЕДНИЕ ПРОСМОТРЕННЫЕ ═══ */}
          <section className="profile-card glass">
            <h2 className="profile-section-title">Последние просмотренные фильмы</h2>
            {isOwner ? (
                historyMovies.length > 0 ? (
                    <div style={{display: 'flex', flexDirection: 'column', gap: '12px', marginTop: '16px'}}>
                      {historyMovies.map((item) => {
                        const movieId = getMovieId(item);
                        const progress = getSavedMovieProgressPercent(movieId);
                        const completed = progress === 0;

                        return (
                            <Link key={movieId} to={`/movie/${movieId}`} style={{textDecoration: 'none'}}>
                              <div style={{
                                display: 'flex', gap: '12px',
                                background: 'rgba(255,255,255,0.04)',
                                borderRadius: '10px',
                                padding: '12px',
                                transition: 'background .2s',
                              }}
                                   onMouseEnter={e => e.currentTarget.style.background = 'rgba(255,255,255,0.08)'}
                                   onMouseLeave={e => e.currentTarget.style.background = 'rgba(255,255,255,0.04)'}
                              >
                                <img
                                    src={getMoviePoster(item)}
                                    alt={getMovieTitle(item)}
                                    style={{width: 52, height: 78, borderRadius: 6, objectFit: 'cover', flexShrink: 0}}
                                />
                                <div style={{minWidth: 0, flex: 1}}>
                                  <div style={{
                                    display: 'flex',
                                    justifyContent: 'space-between',
                                    alignItems: 'flex-start',
                                    gap: '8px'
                                  }}>
                                    <p style={{
                                      margin: 0,
                                      fontWeight: 600,
                                      color: '#fff',
                                      fontSize: '14px',
                                      overflow: 'hidden',
                                      textOverflow: 'ellipsis',
                                      whiteSpace: 'nowrap'
                                    }}>
                                      {getMovieTitle(item)}
                                    </p>
                                    {completed ? (
                                        <span style={{
                                          display: 'flex',
                                          alignItems: 'center',
                                          gap: '4px',
                                          fontSize: '12px',
                                          color: '#5FA670',
                                          flexShrink: 0
                                        }}>
                        <CheckCircle2 size={14}/> Просмотрено
                      </span>
                                    ) : (
                                        <span style={{
                                          fontSize: '12px',
                                          color: '#C9A84C',
                                          flexShrink: 0
                                        }}>{progress}%</span>
                                    )}
                                  </div>
                                  <p style={{margin: '4px 0 6px', fontSize: '12px', color: '#7A7F99'}}>
                                    {getMovieYear(item) || 'Каталог'} · {new Date(item.timestamp).toLocaleString('ru-RU')}
                                  </p>
                                  <div
                                      style={{height: '3px', borderRadius: '2px', background: 'rgba(255,255,255,0.1)'}}>
                                    <div style={{
                                      width: `${progress}%`,
                                      height: '100%',
                                      borderRadius: '2px',
                                      background: '#C9A84C'
                                    }}/>
                                  </div>
                                </div>
                              </div>
                            </Link>
                        );
                      })}
                    </div>
                ) : (
                    <p className="profile-muted">История просмотров пуста.</p>
                )
            ) : (
                <p className="profile-muted">История доступна только владельцу профиля.</p>
            )}
          </section>

          {/* ═══ ИЗБРАННОЕ (watchlist, enriched with posters) ═══ */}
          <section className="profile-card glass">
            <h2 className="profile-section-title">Избранное</h2>
            {isOwner ? (
                watchlistLoading ? (
                    <p className="profile-muted">Загрузка избранного…</p>
                ) : watchlistMovies.length > 0 ? (
                    <MovieGrid movies={watchlistMovies}/>
                ) : (
                    <p className="profile-muted">Список избранного пуст.</p>
                )
            ) : (
                <p className="profile-muted">Список избранного доступен только владельцу профиля.</p>
            )}
          </section>

          {/* ═══ РЕКОМЕНДАЦИИ AI ═══ */}
          <section className="profile-card glass">
            <h2 className="profile-section-title">Рекомендации от AI</h2>
            {recLoading ? (
                <p className="profile-muted">Нейросеть подбирает фильмы…</p>
            ) : personalizedItems.length > 0 ? (
                <MovieGrid movies={personalizedItems}/>
            ) : (
                <p className="profile-muted">Пока нет рекомендаций.</p>
            )}
          </section>

          {/* ═══ ОЦЕНКИ И ОТЗЫВЫ ═══ */}
          <section className="profile-card glass">
            <h2 className="profile-section-title">Оценки и отзывы</h2>
            {!userEmail ? (
                <p className="profile-muted">
                  Перейдите сюда через профиль пользователя, чтобы загрузить оценки.
                </p>
            ) : ratingsLoading ? (
                <p className="profile-muted">Загрузка оценок…</p>
            ) : ratings.length > 0 ? (
                <div style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
                  gap: '14px'
                }}>
                  {ratings.map((r, i) => (
                      <Link
                          key={`${r.movieId}-${i}`}
                          to={`/movie/${r.imdbId || r.movieId}`}
                          style={{textDecoration: 'none'}}
                      >
                        <div style={{
                          display: 'flex', gap: '12px',
                          background: 'rgba(255,255,255,0.04)', borderRadius: '10px',
                          padding: '12px', transition: 'background .2s',
                        }}
                             onMouseEnter={(e) => {
                               e.currentTarget.style.background = 'rgba(255,255,255,0.08)';
                             }}
                             onMouseLeave={(e) => {
                               e.currentTarget.style.background = 'rgba(255,255,255,0.04)';
                             }}
                        >
                          {r.posterUrl && (
                              <img
                                  src={r.posterUrl}
                                  alt={r.title}
                                  style={{width: 52, height: 78, borderRadius: 6, objectFit: 'cover', flexShrink: 0}}
                                  loading="lazy"
                              />
                          )}
                          <div style={{minWidth: 0}}>
                            <p style={{
                              margin: 0,
                              fontWeight: 600,
                              color: '#fff',
                              fontSize: '14px',
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              whiteSpace: 'nowrap'
                            }}>
                              {r.title || `Фильм #${r.movieId}`}
                            </p>
                            <p style={{margin: '4px 0 0', fontSize: '13px', color: '#C9A84C', fontWeight: 600}}>
                              ★ {r.score}/10
                            </p>
                            {r.reviewText && (
                                <p style={{
                                  margin: '4px 0 0',
                                  fontSize: '12px',
                                  color: '#999',
                                  overflow: 'hidden',
                                  textOverflow: 'ellipsis',
                                  display: '-webkit-box',
                                  WebkitLineClamp: 2,
                                  WebkitBoxOrient: 'vertical'
                                }}>
                                  {r.reviewText}
                                </p>
                            )}
                          </div>
                        </div>
                      </Link>
                  ))}
                </div>
            ) : (
                <p className="profile-muted">Нет оценок.</p>
            )}
          </section>

        </div>
      </div>
    </div>
  );
}
