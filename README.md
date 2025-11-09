# 🎬 TestCinema Backend — README

Полный гайд по бэкенду дипломного проекта «интеллектуальная онлайн-платформа для просмотра фильмов с персонализацией и аналитикой рынка киноиндустрии Казахстана».

> Фронтенд вынесен в отдельный репозиторий. Этот README описывает только бэкенд.

---

## Содержание

1. [Краткое описание проекта](#краткое-описание-проекта)
2. [Архитектура и модули](#архитектура-и-модули)
3. [Технологии](#технологии)
4. [Структура проекта](#структура-проекта)
5. [База данных и миграции](#база-данных-и-миграции)
6. [Запуск](#запуск)

   * [Быстрый старт через Docker](#быстрый-старт-через-docker)
   * [Локальный запуск (без Docker)](#локальный-запуск-без-docker)
7. [Конфигурация окружения](#конфигурация-окружения)
8. [Аутентификация и авторизация](#аутентификация-и-авторизация)
9. [REST API](#rest-api)
10. [Интеграции](#интеграции)
11. [Аналитика и рекомендации](#аналитика-и-рекомендации)
12. [Тестирование](#тестирование)
13. [CI/CD и деплой](#cicd-и-деплой)
14. [Производительность и безопасность](#производительность-и-безопасность)
15. [Отладка и частые ошибки](#отладка-и-частые-ошибки)
16. [Дорожная карта](#дорожная-карта)
17. [Лицензия](#лицензия)

---

## Краткое описание проекта

Платформа для просмотра фильмов с:

* каталогом фильмов и жанров,
* пользовательскими аккаунтами и настройками,
* избранным и историей просмотров,
* рейтингами и отзывами,
* магазином мерча,
* подписками и оплатами,
* аналитикой потребления контента,
* интеграцией с OMDb для метаданных,
* базовой персонализацией и планируемым модулем рекомендаций.

Бэкенд отвечает за API, доменную логику, хранение данных и интеграции.

---

## Архитектура и модули

Монолит на Spring Boot 3 с модульной структурой сервисов:

* **Auth & Users**

  * Регистрация и логин (JWT планируется).
  * Профиль пользователя.
  * Настройки пользователя.
  * Роли: `USER`, `ADMIN` (локальный админ доступен).

* **Catalog**

  * Фильмы, жанры, расширенные поля рейтинга.
  * Импорт из OMDb.

* **Ratings & Reviews**

  * Оценки пользователей, агрегация рейтингов.
  * Отзывы (в планах).

* **Merch Store**

  * Товары, заказы, оплаты.

* **Subscriptions & Billing**

  * План подписки, статус, методы оплаты.

* **Analytics**

  * Ежедневные метрики просмотра и активности.

* **Playlists & History**

  * Плейлисты, история, сессии воспроизведения.

* **News & Events**

  * Новости и события платформы.

> В перспективе — выделение в микросервисы (Auth, Catalog, Recommender, Analytics, Billing) и добавление API-шлюза.

---

## Технологии

* **Язык:** Java 17
* **Фреймворк:** Spring Boot 3 (Web, Data JPA, Validation)
* **БД:** PostgreSQL 16
* **Миграции:** Flyway
* **HTTP клиент:** `RestTemplate`/`WebClient` для OMDb
* **Сборка:** Gradle
* **Контейнеризация:** Docker, Docker Compose
* **Кеш/очереди:** планируется Redis
* **Тесты:** JUnit 5

---

## Структура проекта

```
src/
  main/
    java/com/cinema/testcinema/
      config/
        AdminSeeder.java
        SecurityConfig.java
      security/
        AppUserDetailsService.java
        JwtAuthFilter.java
        JwtService.java
      controller/
        AdminAnalyticsController.java
        AuthController.java
        CommentController.java
        DebugController.java
        FavoriteController.java
        GenreController.java
        InternalCheckController.java
        MovieController.java
        ReviewController.java
        SubscriptionController.java
        UserProfileController.java
        UserSettingsController.java
      dto/
        AuthResponse.java
        CommentCreateDto.java
        CommentDto.java
        LoginRequest.java
        MovieDto.java
        ReactionToggleDto.java
        RegisterRequest.java
        ReviewCreateDto.java
        ReviewDto.java
        SubscriptionDto.java
        UserProfileDto.java
        UserSettingsDto.java
      model/
        Achievement.java
        AnalyticsDaily.java
        Comment.java
        CommentReaction.java
        Genre.java
        Movie.java
        PaymentMethod.java
        Reaction.java
        ReactionConverter.java
        Review.java
        Role.java
        Subscription.java
        User.java
        UserAchievement.java
        UserProfile.java
        UserSettings.java
      repository/
        AnalyticsDailyRepository.java
        CommentReactionRepository.java
        CommentRepository.java
        FavoriteRepository.java
        GenreRepository.java
        MovieRepository.java
        ReviewRepository.java
        SubscriptionRepository.java
        UserProfileRepository.java
        UserRepository.java
        UserSettingsRepository.java
      service/
        AuthService.java
        CommentService.java
        FavoriteService.java
        MovieService.java
        OmdbService.java
        ReviewService.java
        SubscriptionService.java
        UserProfileService.java
        UserService.java
        UserSettingsService.java
      TestCinemaApplication.java
    resources/
      application.properties
      db/migration/
        V1_1__core_settings_profile.sql
        V1_2__achievements.sql
        V1_3__subscription_billing.sql
        V1_4__analytics_merch.sql
        V1_5__favorites.sql
        V1_6__history.sql
        V1_7__user_ratings.sql
        V1_8__playlists.sql
        V1_9__news.sql
        V1_10__user_events.sql
        V1_11__watch_sessions.sql
        V2_1__comments_and_reviews.sql
  test/
```

---

## База данных и миграции

* Основная БД: PostgreSQL 16.
* Схема: рекомендуется `public` (или задайте свою в `spring.jpa.properties.hibernate.default_schema`).
* Миграции: Flyway — все DDL/seed в `src/main/resources/db/migration`.

Основные таблицы:

* `users`, `user_profiles`, `user_settings`
* `movies`, `genres`, `movie_genres` (m:n)
* `ratings` (пользовательские оценки)
* `playlists`, `playlist_items`
* `watch_history`, `watch_sessions`, `watch_heartbeats`
* `merch_products`, `merch_orders`
* `subscriptions`, `payment_methods`
* `analytics_daily`
* `achievements`, `user_achievements`
* `news`, `user_events`
* `products`, `orders`, `order_items` (для витрины)

> Поддерживайте миграции через Flyway. Любые изменения схемы — только через новую `Vxxx__*.sql`.

---

## Запуск

### Быстрый старт через Docker

1. Создайте `.env` (см. ниже).
2. Установите Docker Desktop.
3. Запустите:

   ```bash
   docker compose up -d --build
   ```
4. Бэкенд будет доступен на `http://localhost:8080`.

### Локальный запуск (без Docker)

Требуется установленный PostgreSQL 16 и JDK 17.

1. Создайте БД и пользователя:

   ```sql
   CREATE USER test_user WITH PASSWORD 'test_password';
   CREATE DATABASE testdb OWNER test_user;
   ```
2. Обновите `application.properties` или `.env`.
3. Запуск:

   ```bash
   ./gradlew bootRun
   ```
4. Сборка jar:

   ```bash
   ./gradlew clean build
   java -jar build/libs/testcinema-*.jar
   ```

---

## Конфигурация окружения

Создайте файл `.env` в корне:

```dotenv
# Application
APP_PORT=8080
APP_ENV=local

# Postgres
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=testdb
POSTGRES_USER=test_user
POSTGRES_PASSWORD=test_password
POSTGRES_SCHEMA=public

# JPA
SPRING_JPA_HBM2DDL=none           # только Flyway
SPRING_JPA_SHOW_SQL=false

# OMDb
OMDB_API_KEY=put-your-key-here

# Security
JWT_SECRET=local-dev-secret
JWT_TTL_MIN=120

# Admin seed (локально)
ADMIN_USERNAME=admin
ADMIN_PASSWORD=ADMIN
```

`application.properties` может ссылаться на переменные окружения:

```properties
server.port=${APP_PORT:8080}

spring.datasource.url=jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB:testdb}
spring.datasource.username=${POSTGRES_USER:test_user}
spring.datasource.password=${POSTGRES_PASSWORD:test_password}

spring.jpa.hibernate.ddl-auto=${SPRING_JPA_HBM2DDL:none}
spring.jpa.show-sql=${SPRING_JPA_SHOW_SQL:false}
spring.jpa.properties.hibernate.default_schema=${POSTGRES_SCHEMA:public}

spring.flyway.enabled=true
spring.flyway.schemas=${POSTGRES_SCHEMA:public}

omdb.api.key=${OMDB_API_KEY}
```

---

## Аутентификация и авторизация

* **Статус:** базовая регистрация/логин уже есть (`/api/auth/register`, `/api/auth/login`). Переход на **JWT** запланирован.
* **Локальный админ:** пароль `ADMIN` для разработки (из переменных окружения).
* **Роли:** минимум `USER`, `ADMIN`.
* **План:** фильтр JWT, refresh-токены, `@PreAuthorize` на административные эндпоинты, CORS.

---

## REST API

Ниже — ключевые эндпоинты, актуальные на текущем этапе разработки.

### Auth

* `POST /api/auth/register`

  ```json
  { "username": "user1", "password": "pass", "email": "u@ex.com" }
  ```
* `POST /api/auth/login`

  ```json
  { "username": "user1", "password": "pass" }
  ```

  Ответ: `AuthResponse` с токеном или сессией; JWT планируется.

### Users & Settings

* `GET /api/users/{id}` — профиль
* `PUT /api/users/{id}` — обновление профиля
* `GET /api/user-settings/me` — мои настройки
* `PUT /api/user-settings/me` — обновить настройки

### Genres

* `GET /api/genres` — список жанров
* `POST /api/genres` — создать жанр (admin)

### Movies

* `GET /api/movies` — список с фильтрами и пагинацией
* `GET /api/movies/{id}` — детали
* `POST /api/movies` — добавить фильм (admin)
* `POST /api/movies/import/omdb?imdbId=tt....` — импорт из OMDb (admin)

### Ratings

* `POST /api/ratings` — поставить/обновить оценку

  ```json
  { "movieId": 123, "score": 8 }
  ```
* `GET /api/ratings/summary/{movieId}` — агрегаты рейтингов

> Примечание: контроллер сейчас содержит заглушку для `currentUser()` до подключения JWT.

### Merch

* `GET /api/merch/products`
* `POST /api/merch/orders` — оформить заказ

### Subscriptions

* `GET /api/subscriptions/me`
* `PUT /api/subscriptions/me` — смена плана

### История, плейлисты, новости и др.

* Маршруты предусмотрены моделями и DTO. Эндпоинты добавляются итеративно в рамках миграций и релизов.

#### Примеры cURL

```bash
# Регистрация
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo","email":"demo@ex.com"}'

# Логин
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo"}'

# Список фильмов
curl http://localhost:8080/api/movies

# Оценка фильма (после добавления JWT: -H "Authorization: Bearer <token>")
curl -X POST http://localhost:8080/api/ratings \
  -H "Content-Type: application/json" \
  -d '{"movieId":1,"score":9}'
```

---

## Интеграции

### OMDb

* Сервис: `OmdbService`
* Использование:

  * По `imdbId` подтягиваются метаданные (название, год, постер, описание).
  * Сопоставление и сохранение в локальной БД через `MovieService`.
* Требуется `OMDB_API_KEY`.

---

## Аналитика и рекомендации

### Analytics

* Сущность `AnalyticsDaily` копит агрегаты:

  * просмотры, активные пользователи, время просмотра, CTR и др.
* План: фоновые задачи для агрегаций, выгрузка для дашборда на фронтенде.

### Recommender (план)

* Коллаборативная фильтрация на базе `ratings`, `watch_history`, жанров.
* Метрики офлайн-оценки (Precision@K), конфиг `topK`, `shrink`.
* Отдельный модуль или сервис с REST API: `/api/recs/me`.

---

## Тестирование

* Юнит-тесты сервисов: JUnit 5.
* Интеграционные тесты REST с `@SpringBootTest` и Testcontainers PostgreSQL.
* Smoke-набор для CI.

Запуск тестов:

```bash
./gradlew test
```

---

## CI/CD и деплой

* **CI:** GitHub Actions (сборка, тесты, публикация артефакта).
* **CD:** Render/Railway для бэкенда, Vercel/Netlify для фронта.
* **Артефакты:** jar, докер-образы, миграции Flyway применяются на старте.
* **Переменные окружения:** через Secrets.

---

## Производительность и безопасность

### Производительность

* Индексы по ключевым полям (`movie_id`, `user_id`, `created_at`).
* Пагинация и проекции DTO.
* План: кеширование горячих запросов, Redis.

### Безопасность

* Переход на JWT, пароль хранить с BCrypt.
* CORS по списку доменов.
* Валидация входных DTO, лимиты на размеры запросов.
* Секреты только из окружения/Secrets.

---

## Отладка и частые ошибки

1. **`ОШИБКА: схема для создания объектов не выбрана`**
   Решение: укажите схему:

   * В БД:

     ```sql
     ALTER ROLE test_user SET search_path TO public;
     ```
   * Или в `application.properties`:

     ```properties
     spring.jpa.properties.hibernate.default_schema=public
     spring.flyway.schemas=public
     ```

2. **`role "cinema" does not exist` / ошибки доступа**
   Создайте пользователя/БД и дайте права. Проверьте переменные окружения.

3. **HTTP 401 при `/api/auth/login`**
   Проверьте, что `AuthController` и `AuthService` подключены, и фронт шлёт корректный JSON. Для локали доступен админ-логин с паролем `ADMIN` (до включения JWT).

4. **Миграции не применяются**
   Убедитесь, что `spring.flyway.enabled=true`, миграции лежат в `db/migration`, и версия следующая по порядку.

5. **Долгие ответы IDE/скриптов**
   Проверьте:

   * включено ли логирование SQL,
   * нет ли N+1 запросов,
   * выставлены ли индексы,
   * параметры JVM: `-Xms512m -Xmx1g` для локали.

---

## Дорожная карта

* [ ] JWT: access/refresh, роли, guard на эндпоинтах
* [ ] Рейтинги: завершить контроллер, добавить отзывы
* [ ] История/плейлисты: REST и сервисы
* [ ] Analytics: фоновые задачи, экспорт для фронта
* [ ] Рекомендательный сервис: API, офлайн-метрики, A/B
* [ ] Кеширование Redis
* [ ] Webhook платежей и полноценный биллинг
* [ ] Медиахранилище для постеров/трейлеров (S3-совместимое)
* [ ] Документация OpenAPI/Swagger
* [ ] Полный тестовый контур и нагрузочное тестирование

---

## Лицензия

MIT или другая по выбору. Добавьте файл `LICENSE`.

---

### Быстрые ссылки

* **Запуск:** Docker — `docker compose up -d --build`
* **Env:** см. пример `.env`
* **Админ локально:** пароль `ADMIN`
* **OMDb:** задайте `OMDB_API_KEY`

