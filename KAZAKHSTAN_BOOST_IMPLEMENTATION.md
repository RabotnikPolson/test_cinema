# 🎬 Kazakhstan Cinema Boost — Implementation Plan
> Scope: только `rec_ai` (Python) + Frontend. Backend (Java) не трогаем.

---

## Цель

Поднять видимость казахстанского контента **в 3 раза** (×3) относительно текущего уровня через:
1. Усиление boost-коэффициентов в алгоритме
2. Выделенный KZ-эндпоинт с умной сортировкой
3. Фронтенд-секция "Казахстанское кино" с фильтрацией по жанру и году

---

## Фаза 0 — Подготовка (1 день)

### 0.1 Аудит текущего состояния

Проверить, что в БД есть данные для KZ-фильтрации:

```sql
-- Проверяем покрытие полей
SELECT
  COUNT(*) FILTER (WHERE country ILIKE '%казахстан%' OR country ILIKE '%kazakhstan%') AS kz_by_country,
  COUNT(*) FILTER (WHERE language ILIKE '%казахский%' OR language ILIKE '%kazakh%')   AS kz_by_language,
  COUNT(*) FILTER (WHERE is_domestic = true)                                           AS kz_by_flag,
  COUNT(*)                                                                              AS total
FROM movies;
```

Если `kz_by_flag` сильно меньше `kz_by_country` — нужна миграция (см. 0.2).

### 0.2 Опциональная синхронизация `is_domestic`

```sql
-- Выравниваем флаг по country/language без касания Java-бэка
UPDATE movies
SET is_domestic = true
WHERE (
  country ILIKE '%казахстан%' OR country ILIKE '%kazakhstan%' OR country ILIKE '%kz%'
  OR language ILIKE '%казахский%' OR language ILIKE '%kazakh%' OR language ILIKE '%қазақ%'
)
AND is_domestic IS DISTINCT FROM true;
```

> Это чистый SQL — никакой Java-логики не трогаем.

---

## Фаза 1 — rec_ai: усиление буста (1–2 дня)

### 1.1 Изменить константы в `recommender.py`

```python
# ДО (текущее состояние)
KAZAKHSTAN_BOOST = 2.5
CENTRAL_ASIA_BOOST = 1.8
CIS_BOOST = 1.3
DEFAULT_BOOST = 1.0

# ПОСЛЕ (×3 от текущего)
KAZAKHSTAN_BOOST = 3.0   # было 2.5 → стало 3.0
CENTRAL_ASIA_BOOST = 2.2  # было 1.8 → стало 2.2
CIS_BOOST = 1.5           # было 1.3 → стало 1.5
DEFAULT_BOOST = 1.0

# Добавить новые сигнальные константы
KZ_DIRECT_BONUS = 0.35    # Аддитивный бонус к base_score (дополнительно к мультипликатору)
```

### 1.2 Добавить кэш модели (критично для производительности)

```python
from functools import lru_cache
import time

_MODEL_CACHE = {"df": None, "cosine_sim": None, "loaded_at": 0}
MODEL_TTL_SECONDS = 3600  # Перегружать раз в час

def get_recommendations_model_cached():
    now = time.time()
    if _MODEL_CACHE["df"] is None or (now - _MODEL_CACHE["loaded_at"]) > MODEL_TTL_SECONDS:
        df, cosine_sim = get_recommendations_model()
        _MODEL_CACHE["df"] = df
        _MODEL_CACHE["cosine_sim"] = cosine_sim
        _MODEL_CACHE["loaded_at"] = now
    return _MODEL_CACHE["df"], _MODEL_CACHE["cosine_sim"]
```

### 1.3 Обновить `get_smart_hybrid_recommendations` — добавить KZ-аддитив

```python
# В теле функции после расчёта base_score:
kz_boost = get_kazakhstan_boost_score(df.iloc[i])

# Вместо просто умножения — комбо: аддитив + мультипликатор
if kz_boost >= KAZAKHSTAN_BOOST:
    score = base_score * kz_boost + KZ_DIRECT_BONUS
else:
    score = base_score * kz_boost
```

**Почему комбо?** Мультипликатор не помогает, если `base_score` близок к нулю (нет жанрового совпадения). Аддитив гарантирует, что хороший KZ-фильм всегда пробивается в топ.

### 1.4 Обновить `get_kazakhstan_tab_recommendations` — добавить сортировку по релевантности

```python
def get_kazakhstan_tab_recommendations(df, user_id=None, limit=20, genre=None, year_from=None, year_to=None, sort_by="relevance"):
    """
    Новые параметры:
    - year_from, year_to: фильтр по году
    - sort_by: "relevance" | "rating" | "year"
    """
    # ... существующая логика фильтрации ...

    # Фильтр по году (новое)
    if year_from:
        indices = [i for i in indices if _safe_year(kz_df['year'].iloc[i]) and _safe_year(kz_df['year'].iloc[i]) >= year_from]
    if year_to:
        indices = [i for i in indices if _safe_year(kz_df['year'].iloc[i]) and _safe_year(kz_df['year'].iloc[i]) <= year_to]

    # Сортировка (новое)
    if sort_by == "year":
        indices.sort(key=lambda i: _safe_year(kz_df['year'].iloc[i]) or 0, reverse=True)
    elif sort_by == "rating":
        indices.sort(key=lambda i: float(kz_df['rating_norm'].iloc[i]) if 'rating_norm' in kz_df.columns else 0, reverse=True)
    # default: relevance — уже отсортировано по score

    return _format_recs(kz_df, np.array(indices[:limit]), np.array(scores[:limit]), [reasons[0]] * min(limit, len(indices)))
```

### 1.5 Новый эндпоинт в `main.py`

```python
@app.get("/api/v1/recommend/kazakhstan")
def kazakhstan_recommendations(
    user_id: Optional[int] = None,
    limit: int = 20,
    genre: Optional[str] = None,
    year_from: Optional[int] = None,
    year_to: Optional[int] = None,
    sort_by: str = "relevance"  # relevance | rating | year
):
    df, cosine_sim = get_recommendations_model_cached()
    recs = get_kazakhstan_tab_recommendations(
        df, user_id=user_id, limit=limit,
        genre=genre, year_from=year_from, year_to=year_to, sort_by=sort_by
    )
    return {"recommendations": recs, "total": len(recs), "filters": {"genre": genre, "year_from": year_from, "year_to": year_to}}


@app.get("/api/v1/recommend/kazakhstan/genres")
def kazakhstan_genres():
    """Список жанров доступных в KZ-фильмах — для фронтенд-фильтров"""
    df, _ = get_recommendations_model_cached()
    kz_mask = (
        df['country'].str.contains('казахстан|kazakhstan|kz', case=False, na=False) |
        df['language'].str.contains('казахский|kazakh|қазақ', case=False, na=False) |
        df['is_domestic'].fillna(False)
    )
    genres = set()
    for g_str in df[kz_mask]['genre_clean'].dropna():
        for g in g_str.split(','):
            g = g.strip()
            if g:
                genres.add(g)
    return {"genres": sorted(list(genres))}
```

---

## Фаза 2 — Frontend: секция "Казахстанское кино" (2–3 дня)

### 2.1 Архитектура компонента

```
src/
└── components/
    └── KazakhstanSection/
        ├── KazakhstanSection.jsx     ← основной компонент-контейнер
        ├── KazakhstanFilters.jsx     ← жанр + год + сортировка
        ├── KazakhstanMovieCard.jsx   ← карточка с KZ-бейджем
        └── KazakhstanSection.css     ← стили с KZ-брендингом
```

### 2.2 `KazakhstanSection.jsx` — скелет

```jsx
import { useState, useEffect } from "react";
import KazakhstanFilters from "./KazakhstanFilters";
import KazakhstanMovieCard from "./KazakhstanMovieCard";

const REC_AI_BASE = process.env.REACT_APP_REC_AI_URL || "http://localhost:8001";

export default function KazakhstanSection({ userId }) {
  const [movies, setMovies] = useState([]);
  const [genres, setGenres] = useState([]);
  const [filters, setFilters] = useState({ genre: null, year_from: null, year_to: null, sort_by: "relevance" });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(`${REC_AI_BASE}/api/v1/recommend/kazakhstan/genres`)
      .then(r => r.json())
      .then(data => setGenres(data.genres));
  }, []);

  useEffect(() => {
    setLoading(true);
    const params = new URLSearchParams({ limit: 20, sort_by: filters.sort_by });
    if (userId) params.append("user_id", userId);
    if (filters.genre) params.append("genre", filters.genre);
    if (filters.year_from) params.append("year_from", filters.year_from);
    if (filters.year_to) params.append("year_to", filters.year_to);

    fetch(`${REC_AI_BASE}/api/v1/recommend/kazakhstan?${params}`)
      .then(r => r.json())
      .then(data => { setMovies(data.recommendations); setLoading(false); })
      .catch(() => setLoading(false));
  }, [filters, userId]);

  return (
    <section className="kazakhstan-section">
      <div className="kazakhstan-header">
        <span className="kz-flag-icon">🇰🇿</span>
        <h2>Казахстанское кино</h2>
        <span className="kz-subtitle">Казахстанское кино</span>
      </div>

      <KazakhstanFilters
        genres={genres}
        filters={filters}
        onChange={setFilters}
      />

      {loading ? (
        <div className="kz-loading">Загрузка...</div>
      ) : (
        <div className="kz-movie-grid">
          {movies.map(movie => (
            <KazakhstanMovieCard key={movie.movie_id} movie={movie} />
          ))}
        </div>
      )}
    </section>
  );
}
```

### 2.3 `KazakhstanFilters.jsx`

```jsx
export default function KazakhstanFilters({ genres, filters, onChange }) {
  return (
    <div className="kz-filters">
      <select
        value={filters.genre || ""}
        onChange={e => onChange(f => ({ ...f, genre: e.target.value || null }))}
      >
        <option value="">Все жанры</option>
        {genres.map(g => <option key={g} value={g}>{g}</option>)}
      </select>

      <select
        value={filters.sort_by}
        onChange={e => onChange(f => ({ ...f, sort_by: e.target.value }))}
      >
        <option value="relevance">По релевантности</option>
        <option value="rating">По рейтингу</option>
        <option value="year">По году</option>
      </select>

      <div className="kz-year-range">
        <input
          type="number" placeholder="С года" min="1930" max="2025"
          value={filters.year_from || ""}
          onChange={e => onChange(f => ({ ...f, year_from: e.target.value ? +e.target.value : null }))}
        />
        <span>—</span>
        <input
          type="number" placeholder="По год" min="1930" max="2025"
          value={filters.year_to || ""}
          onChange={e => onChange(f => ({ ...f, year_to: e.target.value ? +e.target.value : null }))}
        />
      </div>
    </div>
  );
}
```

### 2.4 `KazakhstanMovieCard.jsx`

```jsx
export default function KazakhstanMovieCard({ movie }) {
  return (
    <div className="kz-movie-card">
      {movie.poster_url ? (
        <img src={movie.poster_url} alt={movie.title} className="kz-poster" />
      ) : (
        <div className="kz-poster-placeholder">🎬</div>
      )}
      <div className="kz-card-body">
        <span className="kz-badge">🇰🇿 Казахстан</span>
        <h3 className="kz-title">{movie.title}</h3>
        {movie.year && <span className="kz-year">{movie.year}</span>}
        {movie.score > 0 && (
          <div className="kz-score-bar">
            <div className="kz-score-fill" style={{ width: `${Math.min(movie.score * 100, 100)}%` }} />
          </div>
        )}
      </div>
    </div>
  );
}
```

### 2.5 Стили `KazakhstanSection.css`

```css
.kazakhstan-section {
  padding: 2rem 0;
}

.kazakhstan-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 1.5rem;
}

.kazakhstan-header h2 {
  font-size: 24px;
  font-weight: 600;
}

.kz-subtitle {
  font-size: 14px;
  color: #888;
}

.kz-badge {
  display: inline-block;
  background: #f0f7ff;
  color: #1a5ca8;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 6px;
}

.kz-movie-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.kz-movie-card {
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #eee;
  transition: transform 0.2s, box-shadow 0.2s;
}

.kz-movie-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
}

.kz-poster {
  width: 100%;
  aspect-ratio: 2/3;
  object-fit: cover;
}

.kz-poster-placeholder {
  width: 100%;
  aspect-ratio: 2/3;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  font-size: 2rem;
}

.kz-card-body {
  padding: 10px 12px;
}

.kz-title {
  font-size: 14px;
  font-weight: 500;
  margin: 4px 0;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.kz-year {
  font-size: 12px;
  color: #888;
}

.kz-score-bar {
  height: 3px;
  background: #eee;
  border-radius: 2px;
  margin-top: 8px;
}

.kz-score-fill {
  height: 100%;
  background: linear-gradient(90deg, #00a0e3, #00c853);
  border-radius: 2px;
}

.kz-filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 1.5rem;
}

.kz-filters select,
.kz-filters input {
  padding: 6px 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 13px;
}

.kz-year-range {
  display: flex;
  align-items: center;
  gap: 6px;
}

.kz-year-range input {
  width: 90px;
}
```

---

## Фаза 3 — Интеграция в главную страницу (0.5 дня)

### 3.1 Добавить секцию на главную

```jsx
// HomePage.jsx или MainFeed.jsx
import KazakhstanSection from "./components/KazakhstanSection/KazakhstanSection";

// В JSX, после основных рекомендаций:
<KazakhstanSection userId={currentUser?.id} />
```

### 3.2 Добавить в навигацию вкладку (опционально)

```jsx
// В TabBar или NavMenu:
<NavLink to="/kazakhstan">
  <span>🇰🇿</span> Казахстанское кино
</NavLink>

// Отдельная страница KazakhstanPage.jsx:
export default function KazakhstanPage() {
  return (
    <div className="page">
      <KazakhstanSection userId={currentUser?.id} showAll={true} />
    </div>
  );
}
```

---

## Фаза 4 — Тестирование (1 день)

### 4.1 Проверка алгоритма

```bash
# Проверить что KZ-фильмы действительно выше в смарт-гибриде
curl "http://localhost:8001/api/v1/recommend/smart/1" | \
  python3 -c "import sys,json; data=json.load(sys.stdin); [print(r['title'], r['score']) for r in data['recommendations']]"

# Проверить новый KZ-эндпоинт
curl "http://localhost:8001/api/v1/recommend/kazakhstan?limit=10&sort_by=rating"

# Проверить фильтр по жанру
curl "http://localhost:8001/api/v1/recommend/kazakhstan?genre=Драма&sort_by=year"

# Получить список жанров
curl "http://localhost:8001/api/v1/recommend/kazakhstan/genres"
```

### 4.2 Что проверить вручную

- [ ] KZ-фильмы появляются в топ-3 при запросе похожих (если в каталоге есть KZ)
- [ ] Фильтр по жанру работает корректно
- [ ] Фильтр по году не ломается при пустых значениях
- [ ] Карточка отображается без poster_url (placeholder)
- [ ] Секция не рендерится пустой (fallback на популярные KZ)

---

## Итоговая сводка изменений

| Файл | Тип изменения | Размер |
|------|--------------|--------|
| `rec_ai/recommender.py` | Константы, кэш, аддитивный буст, новые параметры | ~60 строк |
| `rec_ai/main.py` | 2 новых эндпоинта | ~35 строк |
| `frontend/KazakhstanSection.jsx` | Новый компонент | ~60 строк |
| `frontend/KazakhstanFilters.jsx` | Новый компонент | ~35 строк |
| `frontend/KazakhstanMovieCard.jsx` | Новый компонент | ~30 строк |
| `frontend/KazakhstanSection.css` | Стили | ~80 строк |
| SQL (разовый скрипт) | Синхронизация `is_domestic` | ~10 строк |
| **Java backend** | **не трогаем** | **0 строк** |

**Итого:** ~310 строк нового кода, 0 строк изменений в Java.

---

## Временные оценки

| Фаза | Задача | Время |
|------|--------|-------|
| 0 | Аудит БД + SQL-синхронизация | 2–3 часа |
| 1 | rec_ai: константы + кэш + эндпоинты | 1 день |
| 2 | Frontend компоненты | 1.5 дня |
| 3 | Интеграция в главную | 2–3 часа |
| 4 | Тестирование | 0.5 дня |
| **Итого** | | **~3.5 рабочих дня** |
