# 🎬 Кинопортал — План действий

> **Анализ покрытия:** Java Spring Boot (8080) ↔ Python rec_ai (8000) ↔ React (5173)  
> **Итог:** 4 критических проблемы · 5 средних · 9 мелких

---

## Общий статус

| Компонент | Статус | Проблема |
|---|---|---|
| `AdminAnalyticsController` | ✅ Работает | График активности — захардкоженные данные |
| `MovieController` (все 5 эндпоинтов) | ✅ Работает | — |
| `ReviewService` (CRUD + пагинация) | ✅ Работает | `GET /reviews/{id}` и replies не вызываются |
| `UserSettingsService` | ✅ Работает | Поля `region`/`newsletter` есть в UI, нет в бэкенде |
| `GenreController` — `GET /genres` | ✅ Работает | — |
| `rec_ai` — рекомендации (4 эндпоинта) | ✅ Работает | `user_id` не передаётся в tab-запросах |
| `GenreController` — `POST /genres` | ❌ Не подключён | Нет UI для создания жанров в админке |
| `TrendingController` (2 эндпоинта) | ❌ Не используется | Фронт не вызывает эти эндпоинты |
| `CollectionService` | ❌ Нет контроллера | Методы есть, REST-эндпоинтов нет вообще |
| `MetricsService` | ❌ Нет контроллера | Клики не логируются → Redis пустой → тренды пустые |
| `rec_ai` — `because-you-liked` | ❌ Не вызывается | Эндпоинт есть, фронт его игнорирует |
| `rec_ai` — `/health`, `/stats`, `/ml/retrain` | ❌ Не используется | Нет в админ-панели |

---

## 🔴 Критические проблемы

### 1. MetricsController отсутствует

> **Ключевая цепочка:** без MetricsController → Redis пустой → `/trending/*` пустой → `topByClicks = 0` в аналитике.  
> Начинать нужно строго с этой задачи.

`MetricsService` умеет логировать клики, поиск и субтитры, но у него нет REST-контроллера, и фронтенд его никогда не вызывает.

**Последствия:**
- Redis ZSET для трендов всегда пустой
- `GET /trending` и `GET /trending/weekly` возвращают пустые результаты
- `AdminAnalytics.topByClicks` всегда показывает 0
- Поисковая аналитика не собирается

**Что делать — бэкенд:**
1. Проверить: существует ли `MetricsController` (если нет — создать)
2. Создать эндпоинты: `POST /metrics/click`, `POST /metrics/search`, `POST /metrics/subtitle`

**Что делать — фронтенд:**
1. Вызывать `logClick()` при открытии `MovieDetailsPage`
2. Вызывать `logSearch()` при каждом поисковом запросе (`searchApi.js`)
3. Вызывать `logSubtitleEvent()` при включении/выключении субтитров

---

### 2. TrendingController полностью игнорируется

Бэкенд имеет полноценную систему трендов: агрегация Kinopoisk + клики + domestic-буст через Redis ZSET. Фронтенд берёт тренды из Python AI-сервиса (по IMDb-рейтингу) — это два несвязанных источника.

> **Архитектурная проблема:** Java считает тренды по **реальным кликам**, Python — по **рейтингу IMDb**. Они не связаны между собой. Нужно принять решение: оставить один источник или объединить.

**Что делать:**
1. Сначала выполнить задачу #1 (MetricsController) — иначе тренды будут пустыми
2. Принять решение: Java-тренды или Python-тренды или оба
3. Добавить страницу `/trending` или виджет на главную
4. Вызывать `GET /trending` для общих трендов и `GET /trending/weekly` для еженедельных

---

### 3. SQL Injection в `rec_ai` (recommender.py)

Функции используют f-string для подстановки `movie_id` и `user_id` напрямую в SQL-запросы:

```python
# ❌ Уязвимо
f"SELECT * FROM watch_history WHERE movie_id = {movie_id}"

# ✅ Исправление
"SELECT * FROM watch_history WHERE movie_id = :id", {"id": movie_id}
```

**Затронутые функции:**
- `get_collaborative_users_also_watched()`
- `get_collaborative_recommendations()`
- `get_because_you_liked()`
- `get_smart_hybrid_recommendations()`

**Что делать:**
1. Заменить все f-string SQL на параметризованные запросы через `text()` + `bindparams` или `cursor.execute(query, params)`

---

### 4. Захардкоженный API-ключ Kinopoisk

Файл `rec_ai/seed_kinopoisk_direct.py` содержит API-ключ Kinopoisk прямо в коде:

```python
# ❌ Прямо в коде
API_KEY = "3ee92c30-e913-4af0-ba70-9635a086de50"

# ✅ Исправление
API_KEY = os.getenv("KINOPOISK_API_KEY")
```

**Что делать:**
1. Удалить ключ из кода немедленно
2. Добавить в `.env`: `KINOPOISK_API_KEY=...`
3. Убедиться что `.env` в `.gitignore`
4. Если ключ уже попал в `git history` — считать скомпрометированным и перевыпустить

---

## 🟡 Средний приоритет

### 5. CollectionService без контроллера

`CollectionService.getNewReleases()` и `getTopComedies()` существуют в Java, кешируются через `@Cacheable`, но не имеют REST-эндпоинтов.

**Что делать:**
1. Создать `CollectionController` с эндпоинтами:
   - `GET /collections/new-releases`
   - `GET /collections/top-comedies`
2. Интегрировать на главной странице фронтенда (секции «Новинки» и т.д.)

---

### 6. Нет UI для создания жанров

`POST /genres` (роль ADMIN) существует в бэкенде, но в админ-панели нет никакого интерфейса для создания жанров.

**Что делать:**
1. Добавить форму создания жанра в `AdminMoviesPage` или отдельную страницу
2. Подключить через `moviesApi.js`

---

### 7. `user_id` не передаётся в tab-рекомендациях

`listRecommendationsByTab()` на фронтенде не передаёт `user_id`. Алгоритм `smart` не может исключить уже просмотренные фильмы конкретного пользователя.

**Что делать:**
1. В `recommendationsApi.js` добавить `user_id` к параметрам запроса
2. Получать `user_id` из auth-стора (useAuth hook или аналог)

---

### 8. `because-you-liked` не используется

В `rec_ai` есть `GET /api/v1/recommendations/tab/because-you-liked?user_id=` — персонализация на основе высоких оценок пользователя (≥7 баллов). Фронтенд его не вызывает.

**Что делать:**
1. Добавить вызов в `recommendationsApi.js`
2. Добавить секцию «Потому что вам понравился...» на главную страницу или страницу фильма

---

### 9. Кнопка «Переобучить модель» в AdminAnalyticsPage

ML-модель переобучается только при перезапуске контейнера. Эндпоинт `POST /api/v1/ml/retrain` существует, кнопки в UI нет.

**Что делать:**
1. Добавить кнопку «Переобучить рекомендательную модель» в `AdminAnalyticsPage`
2. При клике вызывать `POST /api/v1/ml/retrain` через `ai-http-client.js`
3. Добавить health-check виджет: `GET /health` показывает статус модели и количество фильмов в памяти

---

## 🟢 Мелкие доработки

| # | Проблема | Что сделать |
|---|---|---|
| 10 | График активности — захардкоженные данные `[120, 150, 180...]` | Создать эндпоинт timeseries из `WatchHistory` или убрать заглушку |
| 11 | Fallback-запрос на `/api/admin/analytics` (путь не существует) | Убрать fallback или добавить alias в бэкенде |
| 12 | Поля `region` и `newsletter` в Settings — нет в бэкенде | Добавить в `UserSettingsUpdateRequest` + миграцию ИЛИ убрать из UI |
| 13 | `GET /reviews/{id}` и `GET /reviews/{id}/replies` не вызываются | Добавить UI для просмотра вложенных ответов (threaded replies) |
| 14 | `isAdmin` определяется частично по email-списку | Перейти на полноценный role-based check |
| 15 | CORS в `rec_ai` только для `localhost:5173/3000/5174` | При деплое обновить список разрешённых origin |
| 16 | Дублирующиеся пути `/recommend/*` и `/recommendations/*` | Пометить `/recommend/*` как deprecated, оставить `/recommendations/*` |
| 17 | `schemas.py` в `rec_ai` пустой | Добавить Pydantic-схемы для валидации ответов |
| 18 | `models.py` не содержит `poster_url`, `year`, `kinopoisk_id` | Добавить поля в ORM или задокументировать, что читается через raw SQL |

---

## 📋 Роадмап (порядок выполнения)

```
Этап 1  ──▶  SQL Injection в rec_ai                    🔴 критично, независимо
Этап 2  ──▶  Убрать API-ключ Kinopoisk из кода         🔴 критично, независимо
Этап 3  ──▶  MetricsController (бэкенд)                🔴 критично, независимо
Этап 4  ──▶  logClick / logSearch (фронтенд)           🔴 зависит от этапа 3
Этап 5  ──▶  Страница /trending (фронтенд)             🔴 зависит от этапов 3, 4
Этап 6  ──▶  CollectionController (бэкенд)             🟡 средний, независимо
Этап 7  ──▶  Коллекции на главной (фронтенд)           🟡 зависит от этапа 6
Этап 8  ──▶  UI для создания жанров в админке          🟡 средний, независимо
Этап 9  ──▶  user_id в tab-рекомендациях               🟡 средний, независимо
Этап 10 ──▶  because-you-liked на фронтенд             🟡 средний, независимо
Этап 11 ──▶  Retrain + health-check в AdminAnalytics   🟡 средний, независимо
Этап 12 ──▶  Мелкие доработки (#10–18)                 🟢 по мере времени
```

---

## 🔗 Зависимости между задачами

```
MetricsController (этап 3)
    └──▶ logClick на фронте (этап 4)
              └──▶ Redis ZSET заполняется
                        └──▶ GET /trending/* работает (этап 5)
                        └──▶ AdminAnalytics.topByClicks перестаёт быть 0

CollectionController (этап 6)
    └──▶ Секции «Новинки» / «Комедии» на главной (этап 7)
```

> Если не сделать **MetricsController** — вся система трендов мертва, даже если `TrendingController` правильно написан.  
> Если не сделать **SQL Injection** — `rec_ai` уязвим к атаке через любой `movie_id` или `user_id` в URL.

---

## Итог

| Приоритет | Кол-во | Задачи |
|---|---|---|
| 🔴 Критические | 4 | SQL Injection, API-ключ, MetricsController, TrendingController |
| 🟡 Средние | 5 | CollectionController, жанры, user_id, because-you-liked, retrain |
| 🟢 Мелкие | 9 | Захардкоженные данные, CORS, deprecated пути и др. |

**Начинать с этапов 1–2** (SQL Injection + API-ключ): исправляются за 1–2 часа, не требуют согласований.  
**Затем этап 3–4** (MetricsController): без него аналитика по кликам не работает.
