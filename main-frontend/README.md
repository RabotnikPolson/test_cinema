PS C:\Users\murat\IdeaProjects\main-frontend\src> tree /F /A
Структура папок
Серийный номер тома: 5ABC-78C6
C:.
|   App.css
|   App.jsx
|   index.css
|   index.jsx
|   main.jsx
|   setupTests.js
|
+---assets
|       no-images.png
|       react.svg
|
+---components
|   |   1.zip
|   |   2.zip
|   |   Header.jsx
|   |   HeroCarousel.jsx
|   |   MovieCard.jsx
|   |   MovieGrid.jsx
|   |   PlayerPlaceholder.jsx
|   |   RatingStars.jsx
|   |   ReviewCard.jsx
|   |   ReviewFormModal.jsx
|   |   ReviewReadModal.jsx
|   |   Sidebar.jsx
|   |
|   +---comments
|   |       CommentItem.jsx
|   |       CommentsSection.jsx
|   |
|   \---RightRail
|           RecommendationsRail.jsx
|
+---entities
|   \---movie
|           mapper.js
|
+---hooks
|       3.zip
|       4.zip
|       useAuth.js
|       useComments.js
|       useFavorites.js
|       useGenres.js
|       useMovie.js
|       useMovies.js
|       useRecommendations.js
|       useReviews.js
|       useSubscription.js
|       useTheme.js
|       useUserProfile.js
|       useUserSettings.js
|
+---layouts
|       AppLayout.jsx
|
+---pages
|       AddMovie.jsx
|       AdminAnalytics.jsx
|       Favorites.jsx
|       Genres.jsx
|       History.jsx
|       Home.jsx
|       Login.jsx
|       MovieDetails.jsx
|       MovieReviews.jsx
|       MovieWatch.jsx
|       ProfilePage.jsx
|       Register.jsx
|       SettingsPage.jsx
|       SubscriptionPage.jsx
|       UserActivityPage.jsx
|
+---shared
|   \---api
|           auth.js
|           comments.js
|           favorites.js
|           http.js
|           movies.js
|           ratings.js
|           recommendations.js
|           reviews.js
|           stream.js
|
+---styles
|   |   global.css
|   |   theme.css
|   |
|   +---components
|   |       comments.css
|   |       Header.css
|   |       HeroCarousel.css
|   |       reviewModal.css
|   |
|   \---pages
|           AddMovie.css
|           AdminAnalytics.css
|           Auth.css
|           Favorites.css
|           Genres.css
|           History.css
|           Home.css
|           MovieDetails.css
|           MovieWatch.css
|           Profile.css
|           Settings.css
|           Subscription.css
|
\---utils
        localHistory.js

PS C:\Users\murat\IdeaProjects\main-frontend\src> 
# 🎬 TestCinema Frontend — README

Полное описание фронтенда дипломного проекта «интеллектуальная онлайн-платформа для просмотра фильмов с персонализацией и аналитикой рынка киноиндустрии Казахстана».

> Бэкенд находится в отдельном репозитории. Здесь описан только клиент.

---

## Содержание

1. [Краткое описание проекта](#краткое-описание-проекта)
2. [Архитектура и модули](#архитектура-и-модули)
3. [Технологии](#технологии)
4. [Структура проекта](#структура-проекта)
5. [Запуск](#запуск)

   * [Быстрый старт (Vite)](#быстрый-старт-vite)
   * [Сборка и предпросмотр](#сборка-и-предпросмотр)
6. [Конфигурация окружения](#конфигурация-окружения)
7. [Аутентификация и авторизация](#аутентификация-и-авторизация)
8. [Работа с API](#работа-с-api)
9. [Аналитика и визуализация данных](#аналитика-и-визуализация-данных)
10. [CI/CD и деплой](#cicd-и-деплой)
11. [Производительность и безопасность](#производительность-и-безопасность)
12. [Отладка и частые ошибки](#отладка-и-частые-ошибки)
13. [Дорожная карта](#дорожная-карта)
14. [Лицензия](#лицензия)

---

## Краткое описание проекта

Фронтенд реализует интерфейс платформы TestCinema — онлайн-сервиса для просмотра фильмов, аналитики и персональных рекомендаций.
Основные возможности:

* каталог фильмов и жанров,
* система избранного и истории,
* аналитика и графики (admin-панель),
* работа с пользовательскими настройками,
* авторизация и регистрация,
* интеграция с backend API.

---

## Архитектура и модули

Frontend — одностраничное приложение (SPA) на React + Vite.
Логика разделена на независимые модули:

* **Core** — App, Router, глобальные стили.
* **Auth** — регистрация, логин, хранение JWT/ролей.
* **Catalog** — список фильмов, карточки, фильтры.
* **Details** — просмотр деталей фильма и отзывов.
* **Favorites / History** — локальные хранилища.
* **Profile / Settings** — пользовательские данные.
* **AdminAnalytics** — отчёты и графики.
* **Merch / Subscription** — магазин и подписки.
* **Search Overlay** — поиск с подсветкой совпадений.

---

## Технологии

* **React 18**, **Vite**
* **React Router 6**
* **React Query** — кэш и асинхронные запросы
* **Axios** — общая обёртка HTTP
* **Chart.js** — визуализация аналитики
* **CSS Modules / глобальные стили**
* **LocalStorage** — хранение токенов, истории, избранного
* **JavaScript (ES2022)** / JSX
* **Node.js 18+**

---

## Структура проекта

```
src/
  components/
    Header.jsx
    Sidebar.jsx
    MovieCard.jsx
    MovieGrid.jsx
    HeroCarousel.jsx
    PrivateRoute.jsx
  pages/
    Home.jsx
    Genres.jsx
    MovieDetails.jsx
    Favorites.jsx
    History.jsx
    AddMovie.jsx
    AdminAnalytics.jsx
    ProfilePage.jsx
    SettingsPage.jsx
    SubscriptionPage.jsx
    Login.jsx
    Register.jsx
  hooks/
    useAuth.js
    useMovies.js
    useMovie.js
    useGenres.js
    useProducts.js
    useSubscription.js
    useUserProfile.js
    useUserSettings.js
    useTheme.js
    useSearch.js
  context/
    HistoryContext.jsx
  api/
    http.js
    movies.js
  utils/
    localHistory.js
    mapMovie.js
  styles/
    index.css
    components/
      Header.css
  App.jsx
  main.jsx
```

---

## Запуск

### Быстрый старт (Vite)

```bash
# установка зависимостей
npm i

# запуск дев-сервера
npm run dev
```

Сайт откроется по адресу [http://localhost:5173](http://localhost:5173)

### Сборка и предпросмотр

```bash
npm run build
npm run preview
```

> Для проверки прод-сборки на локали откройте `http://localhost:4173`.

---

## Конфигурация окружения

Создайте `.env` в корне фронта:

```dotenv
VITE_API_URL=http://localhost:8080
VITE_APP_NAME=TestCinema
VITE_ENABLE_ANALYTICS=true
```

* `VITE_API_URL` — адрес backend API.
* `VITE_APP_NAME` — название приложения.
* `VITE_ENABLE_ANALYTICS` — включение аналитических графиков.

---

## Аутентификация и авторизация

* JWT-токен хранится в `localStorage` (`token`, `role`, `username`).
* При логине вызывается `/api/auth/login`, при регистрации — `/api/auth/register`.
* `PrivateRoute` защищает маршруты, проверяя `useAuth()`.
* Логаут очищает данные и React Query-кэш.

---

## Работа с API

**Axios-инстанс:** `src/api/http.js`

```js
import axios from 'axios';

const http = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  withCredentials: true,
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export default http;
```

**Пример API-модуля:**

```js
import http from './http';

export const getMovies = (params) => http.get('/api/movies', { params });
export const getMovie = (id) => http.get(`/api/movies/${id}`);
export const addMovie = (payload) => http.post('/api/movies', payload);
```

**Типовые эндпоинты:**

* `/api/auth/register`, `/api/auth/login`
* `/api/movies`, `/api/movies/{id}`
* `/api/genres`
* `/api/ratings`
* `/api/merch/products`
* `/api/user-settings/me`

---

## Аналитика и визуализация данных

Раздел **AdminAnalytics** строит графики на **Chart.js**:

* распределение фильмов по годам,
* топ-5 жанров,
* фильтрация по типу контента.

Данные приходят из backend-эндпоинтов `/api/analytics/*`.
Компоненты — `Bar` и `Doughnut` графики с динамическими цветами и легендой.

---

## CI/CD и деплой

**Vercel**

* Import Git Repository
* Build command: `npm run build`
* Output directory: `dist`
* Env vars:

  * `VITE_API_URL=https://api.testcinema.kz`

**Netlify**

* Build: `npm run build`
* Publish directory: `dist`
* Добавьте файл `_redirects`:

  ```
  /* /index.html 200
  ```

**CI (опционально)**: GitHub Actions для авто-билда и деплоя на Vercel.

---

## Производительность и безопасность

**Производительность**

* React Query кэширует данные и предотвращает дубликаты запросов.
* Lazy-loading маршрутов и компонентов.
* Используйте мемоизацию (React.memo, useMemo) в MovieGrid.

**Безопасность**

* Не храните конфиденциальные данные в LocalStorage.
* На продакшене — HTTPS.
* CORS на бэке только для доверенных доменов.
* Очистка HTML в `dangerouslySetInnerHTML` при подсветке поиска.

---

## Отладка и частые ошибки

| Ошибка                                    | Причина                          | Решение                                        |
| ----------------------------------------- | -------------------------------- | ---------------------------------------------- |
| **401 Unauthorized**                      | нет токена или CORS              | проверьте `VITE_API_URL` и заголовки           |
| **Белый экран при перезагрузке страницы** | отсутствует SPA redirect         | добавьте `_redirects` или `vercel.json`        |
| **Пустая история/избранное**              | не совпадает username            | убедитесь, что `useAuth` возвращает верное имя |
| **CORS**                                  | фронт и бэк на разных портах     | настройте `allowedOrigins` на backend          |
| **Search overlay не подсвечивает**        | ошибка в dangerouslySetInnerHTML | проверьте экранирование входных данных         |

---

## Дорожная карта

* [ ] Подключить Swagger-клиент из OpenAPI backend’а
* [ ] Добавить refresh-токен и silent refresh
* [ ] Добавить плейлисты (drag-and-drop UI)
* [ ] Улучшить поиск (дебаунс, подсказки)
* [ ] i18n (ru/kz/en)
* [ ] Темная/светлая темы
* [ ] E2E тесты (Playwright)
* [ ] Персональные рекомендации из AI-сервиса

---

## Лицензия

MIT или иная по выбору. Добавьте файл `LICENSE`.

---

### Быстрые команды

```bash
npm i && npm run dev              # запуск
npm run build && npm run preview  # прод-сборка
```

### Быстрые ссылки

* **Запуск:** `npm run dev`
* **Сборка:** `npm run build`
* **Env:** `.env` с `VITE_API_URL=http://localhost:8080`
* **Хостинг:** Vercel / Netlify
* **Порт:** `5173` (по умолчанию)

---

