# Промпт для разработчика / AI-агента

plan_deystviy.md

проверь этот файл и строго меняй фронтенд и рекомендательную систему.
не трогай бекенд вообще.
исправь все существующие ошибки и критические проблемы, добавь те контроллеры, которые нужны, клики, админскую панель и полностью переделай рекомендательную систему, чтобы он больше упирался на казахский контент и работал великолепно.

Максималбно старайся, чтобы всё работало.
фронтенд - сохрани структуру fsd

Ты опытный full-stack разработчик. Перед тобой кинопортал с тремя слоями: **Java Spring Boot (порт 8080)**, **Python rec\_ai (порт 8000)**, **React (порт 5173)**. Твоя задача — исправить все ошибки и доработать проект строго по инструкциям ниже.

> **ВАЖНО:** Бэкенд на Java Spring Boot не трогаешь вообще. Работаешь только с **фронтендом (React)** и **рекомендательной системой (Python rec\_ai)**.

---

## ЧАСТЬ 1 — Python rec\_ai (recommender.py и всё внутри папки rec\_ai)

### 1.1 Устранить SQL Injection — КРИТИЧНО

Найди все функции, где `movie_id`, `user_id` или любой другой параметр подставляется через f-string прямо в SQL-запрос. Замени каждый такой запрос на параметризованный.

Затронутые функции (минимум):
- `get_collaborative_users_also_watched()`
- `get_collaborative_recommendations()`
- `get_because_you_liked()`
- `get_smart_hybrid_recommendations()`

Пример исправления:
```python
# ДО (уязвимо):
f"SELECT * FROM watch_history WHERE movie_id = {movie_id}"

# ПОСЛЕ (безопасно):
text("SELECT * FROM watch_history WHERE movie_id = :id").bindparams(id=movie_id)
# или через cursor:
cursor.execute("SELECT * FROM watch_history WHERE movie_id = %s", (movie_id,))
```
Пройдись по всему файлу и убедись, что ни одного f-string SQL не осталось.

---

### 1.2 Убрать захардкоженный API-ключ Kinopoisk — КРИТИЧНО

В файле `seed_kinopoisk_direct.py` найди строку вида:
```python
API_KEY = "3ee92c30-e913-4af0-ba70-9635a086de50"
```
Замени на:
```python
import os
API_KEY = os.getenv("KINOPOISK_API_KEY")
if not API_KEY:
    raise ValueError("KINOPOISK_API_KEY not set in environment")
```
Добавь `KINOPOISK_API_KEY=твой_ключ` в файл `.env`. Убедись, что `.env` добавлен в `.gitignore`. Если `.gitignore` не существует — создай его. Считай старый ключ скомпрометированным.

---

### 1.3 Добавить Pydantic-схемы в schemas.py

Файл `schemas.py` пустой. Заполни его Pydantic-схемами для всех эндпоинтов:

```python
from pydantic import BaseModel
from typing import Optional, List

class MovieRecommendation(BaseModel):
    id: int
    title: str
    poster_url: Optional[str]
    year: Optional[int]
    rating: Optional[float]
    genre: Optional[str]
    reason: Optional[str]  # почему рекомендуется

class RecommendationResponse(BaseModel):
    items: List[MovieRecommendation]
    total: int
    tab: Optional[str]

class ClickEvent(BaseModel):
    user_id: int
    movie_id: int
    source: Optional[str] = "unknown"

class SearchEvent(BaseModel):
    user_id: Optional[int]
    query: str

class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
    movies_in_memory: int
    version: str
```
Подключи схемы как типы ответов в роутерах FastAPI (`response_model=...`).

---

### 1.4 Обновить модели ORM в models.py

В `models.py` отсутствуют поля `poster_url`, `year`, `kinopoisk_id`. Добавь их в модель фильма:

```python
poster_url = Column(String, nullable=True)
year = Column(Integer, nullable=True)
kinopoisk_id = Column(String, nullable=True)
```
Если данные берутся через raw SQL а не ORM — задокументируй это явным комментарием в коде.

---

### 1.5 Пометить deprecated-пути

Пути вида `/recommend/*` устарели, актуальные — `/recommendations/*`. В роутере FastAPI добавь на старые пути `deprecated=True`:

```python
@router.get("/recommend/tab", deprecated=True)
```
И верни в теле ответа заголовок или поле `"deprecated": true, "use_instead": "/api/v1/recommendations/tab"`.

---

### 1.6 Обновить CORS

В настройках CORS замени жёстко прописанные `localhost`-адреса на конфигурацию из переменных окружения:

```python
import os
origins = os.getenv("ALLOWED_ORIGINS", "http://localhost:5173").split(",")

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    ...
)
```
В `.env` добавь: `ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000`

---

### 1.7 Полностью переработать рекомендательную систему с упором на казахский контент

Это главная задача. Система должна работать великолепно и ставить казахский контент в приоритет. Реализуй следующее:

**а) Казахстанский буст (Kazakhstan Boost)**

Создай функцию `get_kazakhstan_boost_score(movie)`, которая возвращает коэффициент буста:

```python
KAZAKHSTAN_BOOST = 2.5     # казахский язык или производство
CENTRAL_ASIA_BOOST = 1.8   # Кыргызстан, Узбекистан, Таджикистан, Туркменистан
CIS_BOOST = 1.3            # Россия, Беларусь, Украина, Азербайджан и др.
DEFAULT = 1.0

def get_kazakhstan_boost_score(movie) -> float:
    country = (movie.get("country") or "").lower()
    language = (movie.get("language") or "").lower()
    
    if "казахстан" in country or "kazakhstan" in country or "kz" in country:
        return KAZAKHSTAN_BOOST
    if "казахский" in language or "kazakh" in language or "қазақ" in language:
        return KAZAKHSTAN_BOOST
    if any(c in country for c in ["кыргыз", "узбек", "таджик", "туркмен"]):
        return CENTRAL_ASIA_BOOST
    if any(c in country for c in ["россия", "russia", "беларусь", "украина", "азербайджан"]):
        return CIS_BOOST
    return DEFAULT
```

**б) Новый эндпоинт: казахский контент**

```python
GET /api/v1/recommendations/tab/kazakhstan
```
Параметры: `user_id` (optional), `limit` (default 20), `genre` (optional).

Логика:
1. Из базы достать фильмы с `country ILIKE '%казахстан%' OR country ILIKE '%kazakhstan%' OR language ILIKE '%казахский%'`
2. Если авторизован (`user_id` передан) — исключить уже просмотренные
3. Отсортировать по `rating DESC`, применить буст
4. Вернуть с `reason: "Казахское кино"`

**в) Обновить `smart_hybrid` алгоритм**

В функции `get_smart_hybrid_recommendations()` добавь применение буста ко всем фильмам:

```python
for movie in candidates:
    base_score = movie.get("score", 0) or movie.get("rating", 0) or 0
    boost = get_kazakhstan_boost_score(movie)
    movie["final_score"] = base_score * boost

candidates.sort(key=lambda x: x["final_score"], reverse=True)
```

**г) Обновить `because-you-liked`**

В `get_because_you_liked()`:
1. Убедись что функция принимает `user_id`
2. Достаёт фильмы с оценкой пользователя ≥ 7 баллов
3. Находит похожие по жанру и стране
4. Применяет казахстанский буст
5. Возвращает поле `reason` с названием фильма-источника: `"Потому что вам понравился «Бизнес по-казахски»"`

**д) Улучшить коллаборативную фильтрацию**

В `get_collaborative_recommendations()`:
1. Найти пользователей с похожей историей просмотров
2. Взять фильмы которые они смотрели, но текущий пользователь — нет
3. Применить казахстанский буст к результатам
4. Вернуть топ-20

**е) Добавить эндпоинт `/stats` если его нет**

```python
GET /api/v1/stats
```
Ответ:
```json
{
  "total_movies": 1500,
  "kazakhstan_movies": 120,
  "model_version": "1.2.0",
  "last_retrain": "2024-01-15T10:00:00Z",
  "recommendation_tabs": ["popular", "new", "kazakhstan", "because-you-liked", "smart"]
}
```

**ж) Эндпоинт `/health` — расширить**

```python
GET /health
```
Ответ:
```json
{
  "status": "ok",
  "model_loaded": true,
  "movies_in_memory": 1500,
  "kazakhstan_movies": 120,
  "version": "1.2.0",
  "db_connected": true
}
```

**з) Эндпоинт `POST /api/v1/ml/retrain` — убедиться что работает**

Если не реализован — реализуй:
```python
@router.post("/api/v1/ml/retrain")
async def retrain_model():
    # перезагружает данные из БД и переобучает модель
    await load_model_data()
    return {"status": "retrain_started", "message": "Модель переобучается в фоне"}
```
Запускать переобучение в background task (FastAPI `BackgroundTasks`), не блокируя ответ.

---

## ЧАСТЬ 2 — React фронтенд

> Не трогаешь Spring Boot. Все запросы к Java-бэкенду (`/api/**`) оставляешь как есть. Добавляешь только новые вызовы к `rec_ai` и логику на фронтенде.

---

### 2.1 Логирование кликов и поиска (MetricsService через rec_ai)

Java MetricsController отсутствует, поэтому логирование кликов направляй в `rec_ai`.

Создай или обнови файл `src/api/metricsApi.js`:

```javascript
const REC_AI_URL = import.meta.env.VITE_REC_AI_URL || "http://localhost:8000";

export const logClick = async (userId, movieId, source = "browse") => {
  try {
    await fetch(`${REC_AI_URL}/api/v1/metrics/click`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ user_id: userId, movie_id: movieId, source }),
    });
  } catch (e) {
    // не блокируем UX из-за метрик
    console.warn("logClick failed:", e);
  }
};

export const logSearch = async (query, userId = null) => {
  try {
    await fetch(`${REC_AI_URL}/api/v1/metrics/search`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ query, user_id: userId }),
    });
  } catch (e) {
    console.warn("logSearch failed:", e);
  }
};

export const logSubtitleEvent = async (userId, movieId, action) => {
  try {
    await fetch(`${REC_AI_URL}/api/v1/metrics/subtitle`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ user_id: userId, movie_id: movieId, action }),
    });
  } catch (e) {
    console.warn("logSubtitleEvent failed:", e);
  }
};
```

Подключи в компонентах:
- **`MovieDetailsPage`** — вызывай `logClick(userId, movieId, "details")` в `useEffect` при монтировании
- **`searchApi.js`** — после каждого успешного поиска вызывай `logSearch(query, userId)`
- **Плеер / субтитры** — при включении/выключении субтитров вызывай `logSubtitleEvent(userId, movieId, "on"/"off")`

---

### 2.2 Обновить recommendationsApi.js — передавать user_id везде

Найди файл `src/api/recommendationsApi.js`. В функции `listRecommendationsByTab()` добавь `user_id` к параметрам запроса:

```javascript
export const listRecommendationsByTab = async (tab, options = {}) => {
  const { userId, limit = 20, genre } = options;
  const params = new URLSearchParams({ tab, limit });
  if (userId) params.append("user_id", userId);
  if (genre) params.append("genre", genre);

  const res = await fetch(
    `${REC_AI_URL}/api/v1/recommendations/tab?${params}`
  );
  return res.json();
};

export const getBecauseYouLiked = async (userId, limit = 10) => {
  const res = await fetch(
    `${REC_AI_URL}/api/v1/recommendations/tab/because-you-liked?user_id=${userId}&limit=${limit}`
  );
  return res.json();
};

export const getKazakhstanRecommendations = async (userId = null, limit = 20) => {
  const params = new URLSearchParams({ limit });
  if (userId) params.append("user_id", userId);
  const res = await fetch(
    `${REC_AI_URL}/api/v1/recommendations/tab/kazakhstan?${params}`
  );
  return res.json();
};

export const getTrending = async (weekly = false) => {
  const endpoint = weekly ? "/trending/weekly" : "/trending";
  const res = await fetch(`${REC_AI_URL}/api/v1${endpoint}`);
  return res.json();
};
```

`userId` получай из auth-стора — через хук `useAuth()` или аналог, который уже используется в проекте.

---

### 2.3 Главная страница — добавить секции

На главной странице (`HomePage` или аналог) добавь следующие секции в этом порядке:

1. **«Казахское кино»** — горизонтальный скролл-список, данные из `getKazakhstanRecommendations(userId)`. Если пользователь авторизован — персонализировано, иначе — топ казахских фильмов по рейтингу.

2. **«Потому что вам понравился...»** — показывать только авторизованным пользователям. Данные из `getBecauseYouLiked(userId)`. Заголовок секции брать из поля `reason` первого элемента ответа.

3. **«В тренде»** — горизонтальный скролл-список. Данные из `getTrending()`.

4. **«Новинки»** и **«Топ комедии»** — если в проекте уже есть `CollectionController` на Java — оставь как есть. Если нет — получай данные из `rec_ai` через tab-рекомендации с параметром `tab=new` и `tab=comedies`.

Каждая секция:
- Показывает скелетон-лоадер пока данные грузятся
- При ошибке загрузки — скрывается (не показывает ошибку пользователю, логирует в консоль)
- Карточка фильма кликабельна → переход на `MovieDetailsPage` + вызов `logClick()`

---

### 2.4 Страница /trending

Создай страницу `src/pages/TrendingPage.jsx`:

- Роут: `/trending`
- Две вкладки: **«Сейчас популярно»** и **«За неделю»**
- Данные: `getTrending(false)` и `getTrending(true)` соответственно
- Сетка карточек фильмов (3–4 в ряд на десктопе, 2 на планшете, 1 на мобиле)
- При клике на карточку — `logClick()` + переход на детальную страницу
- Добавить ссылку на `/trending` в навигацию (хедер или сайдбар)

---

### 2.5 Обновить AdminAnalyticsPage

Найди `AdminAnalyticsPage` и добавь следующее:

**а) Кнопка «Переобучить модель»**
```jsx
const handleRetrain = async () => {
  setRetraining(true);
  try {
    const res = await fetch(`${REC_AI_URL}/api/v1/ml/retrain`, { method: "POST" });
    const data = await res.json();
    showNotification("Переобучение запущено в фоне");
  } catch (e) {
    showNotification("Ошибка запуска переобучения", "error");
  } finally {
    setRetraining(false);
  }
};

<button onClick={handleRetrain} disabled={retraining}>
  {retraining ? "Запускается..." : "Переобучить рекомендательную модель"}
</button>
```

**б) Виджет статуса AI-модели**

Добавь блок в шапку AdminAnalyticsPage:
```jsx
// При монтировании:
const healthData = await fetch(`${REC_AI_URL}/health`).then(r => r.json());

// Отображение:
<div className="ai-health-widget">
  <span>AI-модель: {healthData.status === "ok" ? "✅ Работает" : "❌ Недоступна"}</span>
  <span>Фильмов в памяти: {healthData.movies_in_memory}</span>
  <span>Казахских фильмов: {healthData.kazakhstan_movies}</span>
  <span>Версия: {healthData.version}</span>
</div>
```

**в) Исправить захардкоженный график активности**

Найди массив вида `[120, 150, 180, ...]` в коде аналитики. Замени на запрос к Java-бэкенду:
```javascript
// Попробовать реальный эндпоинт:
const res = await fetch("/api/admin/analytics/activity-timeseries");
// Если 404 — скрыть график и показать заглушку "Данные ещё собираются"
if (!res.ok) {
  setActivityData(null); // компонент рендерит плейсхолдер вместо фейковых данных
  return;
}
```
Не показывай фейковые данные пользователю. Либо реальные данные, либо честная заглушка.

**г) Убрать fallback на `/api/admin/analytics`**

Найди в коде fallback-запрос на несуществующий путь `/api/admin/analytics` (без уточняющего эндпоинта). Удали его. Используй только конкретные эндпоинты.

---

### 2.6 Добавить UI для создания жанров в админке

Найди `AdminMoviesPage` или страницу управления контентом. Добавь форму:

```jsx
const [newGenre, setNewGenre] = useState("");

const handleCreateGenre = async () => {
  if (!newGenre.trim()) return;
  try {
    await fetch("/api/genres", {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({ name: newGenre.trim() }),
    });
    setNewGenre("");
    fetchGenres(); // обновить список
    showNotification("Жанр создан");
  } catch (e) {
    showNotification("Ошибка создания жанра", "error");
  }
};

// JSX:
<div className="create-genre-form">
  <h3>Добавить жанр</h3>
  <input
    value={newGenre}
    onChange={e => setNewGenre(e.target.value)}
    placeholder="Название жанра"
    onKeyDown={e => e.key === "Enter" && handleCreateGenre()}
  />
  <button onClick={handleCreateGenre}>Создать жанр</button>
</div>
```

---

### 2.7 Исправить role-based проверку isAdmin

Найди место где `isAdmin` определяется через список email-адресов. Замени на проверку роли из JWT или объекта пользователя:

```javascript
// ДО (плохо):
const isAdmin = ["admin@cinema.kz", "superadmin@cinema.kz"].includes(user.email);

// ПОСЛЕ (правильно):
const isAdmin = user?.role === "ADMIN" || user?.roles?.includes("ADMIN");
```
Если бэкенд возвращает роли в другом формате — адаптируй под реальную структуру ответа, но уйди от email-списка.

---

### 2.8 Добавить секцию threaded replies для рецензий

На `MovieDetailsPage` в блоке рецензий:
1. Для каждой рецензии добавь кнопку «Показать ответы (N)»
2. При клике — вызывай `GET /reviews/{id}/replies` через существующий `reviewsApi.js`
3. Показывай вложенные ответы с отступом
4. При первом рендере — не вызывать, только по клику (lazy load)

---

### 2.9 Поля region и newsletter в Settings

Найди `SettingsPage` / `UserSettingsPage`. Поля `region` и `newsletter` есть в UI, но бэкенд их не принимает.

Реши так: **скрой поля** (`display: none` или условный рендер `{false && <...>}`) и добавь комментарий в коде:
```jsx
{/* TODO: region и newsletter не поддерживаются бэкендом, скрыто до реализации */}
```
Не отправляй эти поля в запросе к `/api/user/settings` — они будут игнорироваться или вызывать ошибку.

---

## ЧАСТЬ 3 — Финальный чеклист

После выполнения всех задач убедись:

**rec_ai:**
- [ ] Ни одного f-string SQL в recommender.py
- [ ] API-ключ Kinopoisk читается из env
- [ ] `.env` в `.gitignore`
- [ ] schemas.py заполнен Pydantic-схемами
- [ ] Эндпоинт `/api/v1/recommendations/tab/kazakhstan` работает
- [ ] `because-you-liked` принимает `user_id`, возвращает `reason`
- [ ] `smart_hybrid` применяет казахстанский буст
- [ ] `/health` возвращает `kazakhstan_movies` и `model_loaded`
- [ ] `POST /api/v1/ml/retrain` запускает Background Task
- [ ] CORS читается из env
- [ ] Старые пути `/recommend/*` помечены deprecated

**React:**
- [ ] `logClick()` вызывается на MovieDetailsPage
- [ ] `logSearch()` вызывается при каждом поиске
- [ ] `listRecommendationsByTab()` передаёт `user_id`
- [ ] Секция «Казахское кино» на главной
- [ ] Секция «Потому что вам понравился...» для авторизованных
- [ ] Секция «В тренде» на главной
- [ ] Страница `/trending` создана и добавлена в навигацию
- [ ] Кнопка «Переобучить модель» в AdminAnalyticsPage
- [ ] Виджет здоровья AI в AdminAnalyticsPage
- [ ] График активности не показывает захардкоженные данные
- [ ] Форма создания жанра в админке работает
- [ ] `isAdmin` проверяет роль, не email
- [ ] Поля region/newsletter скрыты
- [ ] Threaded replies в рецензиях работают по клику

---

## Технические ограничения

- Java Spring Boot (`localhost:8080`) — **не трогать вообще**
- Все переменные окружения — в `.env`, читать через `import.meta.env.VITE_*` на фронте и `os.getenv()` в Python
- Все fetch-вызовы к `rec_ai` оборачивать в `try/catch` — ошибки не должны ломать UI
- Казахстанский буст применяется везде: в hybrid, collaborative, because-you-liked и kazakhstan-tab
- Секции на главной скрываются при ошибке загрузки, не показывают сообщение об ошибке пользователю