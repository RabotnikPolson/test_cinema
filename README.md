# testCinema

**testCinema** — это полнофункциональный каталог кино с поддержкой ИИ, микросервисной архитектурой для перевода и векторизации субтитров, интеграцией с внешними API (Кинопоиск, TMDB) и инструментами для взаимодействия пользователей (рейтинги, отзывы, комментарии).

Проект состоит из классического монолитного бэкенда на Spring Boot 3, современного SPA-фронтенда на React/Vite и микросервисов на Python для обработки ИИ-вычислений.

## 📦 Архитектура проекта

Проект использует трехуровневую архитектуру на стороне бэкенда, разделенную с Python AI-воркерами и React-клиентом:

- **Backend (Spring Boot 3 + Java 17)**: Ядро приложения, обеспечивающее REST API, бизнес-логику, интеграцию с БД (PostgreSQL), JWT-авторизацию и планирование задач (очереди скачивания). Обрабатывает фильмы, пользователей, жанры и связывается с микросервисом перевода.
- **Frontend (React 18 + Vite)**: Находится в папке `main-frontend`. Отвечает за пользовательский интерфейс приложения. Связывается с основным бэкендом посредством REST API (используется Axios, React Query) и проверяет права доступа на основе JWT.
- **Микросервис Перевода (Python / FastAPI)**: Находится в папке `subtitle-translator`. Занимается переводом `.vtt` субтитров (например с английского или русского на казахский), применяет различные AI-провайдеры (OpenAI, Gemini).
- **AI Worker (Python / FastAPI)**: Находится в папке `python-ai`. Использует библиотеки, такие как `sentence-transformers`, для создания векторных вложений (embeddings) из субтитров фильмов с целью умного ИИ-поиска.

## 🚀 Ключевые возможности

1. **Интеграции с API**
   - **Кинопоиск**: Поиск и добавление информации о фильмах (метаданные, постеры, жанры) с помощью API `kinopoisk.dev`.
   - **TMDB**: Получение `tmdbId` в качестве резервного ID для фильмов (используется для поиска субтитров).
   - **OpenSubtitles**: Автоматический поиск, запрос и загрузка `.vtt` субтитров для фильмов во внутреннее хранилище.

2. **Пайплайн субтитров (Java ↔ Python)**
   - Поиск субтитров с нужным языком для фильма.
   - Скачивание субтитра рабочим процессом в Java и сохранение локально (`./storage/subtitles/`).
   - Инициация перевода (Java отправляет запрос к Python `subtitle-translator` на эндпоинт `/api/translate`).
   - Возврат результата через вебхук `POST /api/internal/subtitles/translation-complete`.
   - Векторизация и внедрение субтитров через `python-ai` worker.

3. **Аутентификация и пользователи**
   - Полноценный JWT (Access + Refresh tokens).
   - Функционал регистрации, логина, профиля, настроек, системы ролей (ADMIN vs USER).
   - Хранение паролей в зашифрованном виде, блокированные пользователи (по состоянию `enabled`).

4. **Система комментариев и оценок**
   - Рейтинги фильмов, добавление в watchlist.
   - История просмотров, профили пользователей (`/dto/profile/`, `/dto/settings/`).
   - Рецензии (и реакции на них), и древовидная система комментариев.

## 🛠 Технологический стек

- **Бэкенд:** Java 17, Spring Boot 3.5.6, Spring Security, Spring Data JPA, JWT, Flyway.
- **База данных:** PostgreSQL 16 (запускается через Docker).
- **Фронтенд:** React 18, Vite, React Router 6, React Query, JS, Chart.js.
- **ИИ / Микросервисы:** Python 3.10+, FastAPI, `sentence-transformers`, интеграции OpenAI/Gemini API.

## ⚙️ Установка и запуск (Локально)

**1. Инфраструктура (База данных)**

Запустите контейнеры базы данных в Docker:
```bash
docker-compose up -d
```

**2. Конфигурация бэкенда (`application.properties`)**

Создайте или настройте переменные среды (можно задать их в IDE или через `application-local.properties`):
- `DB_PASSWORD` (пароль к БД, задается через переменные окружения или .env)
- `KINOPOISK_API_KEY` (ваш ключ для API Кинопоиска)
- `TMDB_API_TOKEN` (ключ для TMDB)
- `JWT_SECRET` (секретный ключ для генерации токенов в Spring)
- Учетные данные для OpenSubtitles (`opensubtitles.api.key`, `username`, `password`)
- `INTERNAL_SECRET` / `ai.translator.secret` (секрет для связи Java бэкенда с Python микросервисом)

**3. Запуск основного Java Backend**
```bash
# Сборка проекта
./gradlew build

# Запуск приложения
./gradlew bootRun
```
*API-документация с Swagger UI будет доступна по адресу `http://localhost:8080/swagger-ui.html`*

**4. Запуск Frontend (React/Vite)**
```bash
cd main-frontend
npm install
npm run dev
```

**5. Запуск микросервиса перевода (Опционально)**
```bash
cd subtitle-translator
python -m venv venv
source venv/bin/activate  # Для Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --host localhost --port 8100 --reload
```
Убедитесь, что `INTERNAL_SECRET` совпадает с `ai.translator.secret` на стороне Java.

## 📁 Структура базы данных
База данных управляется с помощью паттерна миграций `Flyway` (папка `src/main/resources/db/migration/`). При запуске Spring всегда проверяет и накладывает новые миграции `V*.sql`. Главные сущности:
- `users`, `user_roles`, `user_profiles`, `refresh_tokens`.
- `movies`, `genres`, `movie_genres`, `movie_subtitles` (очереди загрузок и перевода субтитров).
- `watch_history`, `watchlists`, `ratings`.
- `reviews`, `comments`, реакции.

## 🧪 Тестирование
```bash
# Интеграционные тесты для Backend (H2-подобные тесты с использованием Postgres / Flyway)
./gradlew test
```

## 🤝 Вклад в проект

При расширении проекта обратите внимание на файл [AGENTS.md](./AGENTS.md) и [DESIGN.md](./DESIGN.md)
для глубокого понимания архитектурных и дизайн-паттернов, связанных со строгими правилами добавления нового кода.


