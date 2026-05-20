# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Java Backend (Spring Boot)
```bash
./gradlew build          # compile + run tests
./gradlew bootRun        # start on :8080
./gradlew test           # run all tests
./gradlew test --tests "com.cinema.testcinema.SomeTest"  # single test
```

### Infrastructure
```bash
docker-compose up -d     # start Postgres, Redis, MinIO, AI service, subtitle-translator, frontend
docker-compose up -d postgres redis minio  # infra only (run Java locally)
```

### Frontend (React + Vite)
```bash
cd main-frontend && npm install && npm run dev   # :5173
```

### AI Recommendation Service
```bash
cd test_ai/test_ai && uvicorn main:app --reload --port 8000
```

### Subtitle Translator
```bash
cd subtitle-translator && uvicorn main:app --host localhost --port 8100 --reload
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Required Environment Variables

Create `.env` in the project root (loaded via `spring.config.import=optional:file:.env[.properties]`):

```
DB_PASSWORD=
KINOPOISK_API_KEY=        # kinopoisk.dev — 500 req/day and 20req/sec free tier
TMDB_API_TOKEN=
OPENSUBTITLES_API_KEY=  # 20 downloads/day free tier
OPENSUBTITLES_USERNAME=
OPENSUBTITLES_PASSWORD=
JWT_SECRET=
GEMINI_API_KEY=           # for subtitle-translator #free tier
OPENAI_API_KEY=           # for subtitle-translator #pay as you-go
```

## Architecture

### Services Map
| Service | Port | Stack | Role |
|---|---|---|---|
| Java backend | 8080 | Spring Boot 3 / JPA / Flyway | Main API, business logic |
| AI service | 8000 | FastAPI / scikit-learn | ML recommendations |
| Subtitle translator | 8100 | FastAPI / OpenAI / Gemini | `.vtt` → Kazakh translation |
| PostgreSQL | 5432 | pg16 | Shared DB (Java + AI service both connect directly) |
| Redis | 6379 | redis:7.2 | Cache (`@Cacheable`) + trending sorted sets |
| MinIO | 9000 | minio | Video/subtitle file storage (S3-compatible) |
| Frontend | 5173 | React 18 / Vite | SPA |

### Java Package Layout
```
auth/           — register/login, JWT issue & refresh (AuthController)
security/       — JwtAuthenticationFilter, JwtService, UserPrincipal
config/         — SecurityConfig, RedisConfig, JpaConfig
controller/     — REST endpoints (one per domain)
service/        — business logic
model/          — JPA entities
dto/            — request/response records (nested by domain)
repository/     — Spring Data JPA repos + MovieFilterRepository (custom JPQL)
client/         — HTTP clients: KinopoiskClient, TmdbClient, OpenSubtitlesClient
exception/      — BusinessException + RestExceptionHandler (@ControllerAdvice)
```

### Database Migrations
Flyway runs automatically on startup. Migrations are in `src/main/resources/db/migration/V*.sql`. `ddl-auto=validate` — Hibernate validates against the schema but never alters it. Add new migrations as `V{N+1}__description.sql`.

### Security Model
Stateless JWT. `SecurityConfig` defines public vs. authenticated rules:
- Public GET: `/movies/**`, `/genres/**`, `/reviews/**`, `/trending/**`, `/collections/**`
- Public POST: `/metrics/**` (anonymous analytics)
- Internal (no auth check): `/api/internal/**` — used by subtitle-translator webhook
- Everything else: `authenticated()`

`@EnableMethodSecurity` is on, so `@PreAuthorize` can be used in services for ADMIN-only operations.

### Subtitle Pipeline (Java ↔ Python)
1. Java `SubtitleDownloadWorker` finds a subtitle via OpenSubtitles (using TMDB ID) and saves the `.vtt` to `./storage/subtitles/`
2. Java calls `subtitle-translator` at `POST /api/translate` with the file path
3. Translator (OpenAI Batch API or Gemini) translates to Kazakh asynchronously, then POSTs back to `POST /api/internal/subtitles/translation-complete` (webhook)
4. Java `SubtitleTranslationWebhookController` updates `movie_subtitles.translation_status`

The translator has its own SQLite state DB (`subtitle-translator/data/translator_state.db`) for job tracking and webhook retry logic.

### AI Recommendation Service
`test_ai/test_ai/` — FastAPI app that reads the same PostgreSQL DB as Java (direct connection, not via API). On startup it builds an in-memory content-based similarity matrix from `movies`, `ratings`, and `watch_history` tables.

Key endpoints called by Java backend:
- `GET /api/v1/recommend/tab/smart/{movie_id}?user_id={id}` — primary recommendation
- `GET /api/v1/recommend/feed/{user_id}` — personalized feed
- `GET /api/v1/recommend/tab/because-you-liked/{user_id}`
- `POST /api/v1/ml/retrain` — trigger background model reload

### Trending (Redis)
`TrendingRedisService` maintains a Redis sorted set per ISO week (`trending:movies:YYYY-WNN`). Every movie card click increments the score via `MetricsService.logClick()`. `TrendingController` reads the top-N IDs and fetches movie data from DB.

### Caching Strategy
`@Cacheable` with Redis (24h TTL by default). Cache names in use: `kinopoisk_film`, `kinopoisk_search`, `kinopoisk_top`, `kinopoisk_similars`, `kinopoisk_staff`, `kinopoisk_facts`, `kinopoisk_reviews`, `home_collections`. Evict caches manually via Redis CLI when underlying data changes.

### Analytics (AI Training Data)
Three tables feed the ML model (all nullable `user_id` to support anonymous users):
- `movie_clicks` — implicit feedback (card opens)
- `search_logs` — query intent signals
- `subtitle_events` — Kazakh language affinity signal

CORS is locked to `http://localhost:5173`. Update `SecurityConfig.corsConfigurationSource()` for production domains.

## Workflow (mandatory, follow every time)

This is a required sequence. Do not skip steps.

1. **Discussion** — When given a task, ask clarifying questions first. Make sure we fully understand each other: task boundaries, expected behavior, edge cases.
2. **Plan** — After discussion, propose a step-by-step implementation plan. Wait for my explicit approval ("yes", "ok", "go ahead") before starting any work.
3. **Implementation** — Work step by step. After each step:
    - Show what changed
    - Provide testing instructions: which requests to send (curl/Postman), what response to expect, what to check in the DB if needed
4. **Final** — After all steps are done, provide a complete live-test guide for the entire feature from A to Z.

## Do Not Touch Without My Approval
- Do not modify existing Flyway migrations (only add new ones)
- Do not modify `SecurityConfig` (endpoint access rules)
- Do not modify `application.properties` (DB settings, JWT, port)
- Do not delete existing endpoints or change their contracts
- Do not add new dependencies to `build.gradle`
- Do not modify DB schema without discussion
- Do not push to remote or create PRs
- Do not modify Docker Compose configuration