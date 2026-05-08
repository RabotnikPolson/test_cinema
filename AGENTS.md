# AGENTS.md — Guiding AI Agents in testCinema

A Spring Boot 3 REST API for a cinema catalog with AI-powered features, external API integrations, and user engagement tools.

## Architecture Overview

**Stack:** Spring Boot 3.5.6 + PostgreSQL + Flyway migrations + JWT security + Java 17

**Three-tier structure:**
- **Controllers** (`/controller/`): REST endpoints with OpenAPI docs (springdoc-openapi 2.8.13)
- **Auth controllers** (`/auth/`): registration/login/refresh/logout JWT flow
- **Services** (`/service/`): Business logic, external integrations, scheduled workers
- **JPA Repositories** (`/repository/`): Spring Data JPA for database operations
- **Models** (`/model/`): JPA entities with rich relationships (movies, users, genres, ratings, reviews, watch history, profiles, settings)
- **Infrastructure** (`/client/`, `/config/`, `/security/`, `/user/`): HTTP clients, Spring config, JWT/security adapters, `UserPrincipal` mapping

**Critical add-on:** Subtitle processing now has three parts: Java discovery/download (`SubtitleMetadataService`, `SubtitleDownloadWorker`, `MovieSubtitle`), Python translation microservice (`/subtitle-translator/`), and async Python vectorization worker (`/python-ai/`) using `sentence-transformers`.

## Key Integration Points

### 1. Kinopoisk API Integration
- **Files:** `KinopoiskClient.java`, `KinopoiskSyncService.java`, `MovieController.addFromKinopoisk()`
- **Pattern:** Fetch movie metadata by ID → cache in Movie entity → sync genres via M2M table `movie_genres`
- **Key field:** `kinopoiskId` (Kinopoisk ID), `kinopoiskHdId` (alternative ID)
- **Rate limits:** 500 req/day, 20 req/sec (FREE tier from kinopoisk.dev)
- **Caching:** All movie data persisted in DB after first API call; subsequent requests return cached data

### 2. Stream Embed Integration
- **Files:** `controller/StreamController.java` (currently fully commented out), `config/SecurityConfig.java`
- **Pattern:** Historical implementation resolved movie by internal ID → if `kinopoiskId` exists returned `https://vbdkv.com/api/short/{kinopoiskId}` → otherwise built a `vidsrc` embed URL by `imdbId`
- **Validation:** The commented implementation validated optional params `ds_lang`, `autoplay`, `sub_url` before URL construction
- **Security:** `SecurityConfig` still whitelists `/stream/**` and `/movies/*/stream`, but there is no active stream controller in `src/main/java`

### 3. Subtitle Download/Translation/Vectorization Pipeline (Java ↔ Python)
- **Java side:** Active metadata/download pipeline exists in `SubtitleMetadataService` + `SubtitleDownloadWorker` with `MovieSubtitle` / `MovieSubtitleRepository`, plus translation webhook handling in `SubtitleTranslationWebhookController`
- **Discovery pattern:** `SubtitleMetadataService.discoverForMovie()` performs waterfall search `kk → ru (for CIS country) → en` via `OpenSubtitlesClient.searchSubtitles(...)` and saves `os_file_id` queue records in `movie_subtitles`
- **Download pattern:** `SubtitleDownloadWorker.processDownloadQueue()` takes first pending subtitle, calls `OpenSubtitlesClient.requestDownloadLink(...)`, saves bytes to `storage/subtitles/{movieId}/{lang}.vtt`, marks `is_downloaded=true`, sets `translation_status='pending'`, then calls `SubtitleTranslationClient.triggerTranslation(...)`
 - **Download pattern:** `SubtitleDownloadWorker.processDownloadQueue()` takes first pending subtitle, calls `OpenSubtitlesClient.requestDownloadLink(...)`, saves bytes to `storage/subtitles/{movieId}/{lang}.vtt`, marks `is_downloaded=true`, sets `translation_status='pending'`, then calls `SubtitleTranslationClient.triggerTranslation(...)`.
   - Note: the worker converts the saved relative `local_path` to an absolute, forward-slash-normalized path before calling the translator to avoid working-directory differences between Java and the Python translator. It also synthesizes an absolute `output_path` that ends with `kk.srt` (Kazakh output) when requesting translation.
- **Triggering:** Manual endpoints are provided in `controller/SubtitleTestController.java` (`POST /api/test/subtitles/discover/{movieId}`, `POST /api/test/subtitles/trigger-worker`)
- **Java callback endpoint:** `POST /api/internal/subtitles/translation-complete` updates `movie_subtitles.translation_status`, `translated_path`, `lines_translated` (guarded by `X-Internal-Secret`; current webhook statuses are `success`, `partial`, `failed`)
  - Implementation note: the webhook controller queries `movie_subtitles` by `movie_id` and updates the first matching row's `translation_status`/`translated_path`/`lines_translated` fields (it does not currently create a new row for the translated file).
- **Scheduling note:** Worker method is annotated with `@Scheduled(fixedDelay = 1800000)`, but `TestCinemaApplication` currently has no `@EnableScheduling`
- **Python translation side:** `subtitle-translator/routes/translate.py` exposes `POST /api/translate` (queued), and `subtitle-translator/services/webhook_client.py` posts results back to Java internal webhook
- **Python vectorization side:** `python-ai/workers/worker.py` still uses `_mock_download_from_s3(...)` and APScheduler every 5 minutes for vectorization (`paraphrase-multilingual-MiniLM-L12-v2`, 384 dims)
- **Schema mismatch warning:** Java schema now has `os_file_id/local_path/is_downloaded` + `translation_status/translated_path/lines_translated` (V23), while `python-ai` models still expect `movie_subtitles.s3_path/format/is_vectorized_for_ai`
 - **Python vectorization side:** `python-ai/workers/worker.py` still uses `_mock_download_from_s3(...)` (a mocked S3/VTT fetch) and APScheduler every 5 minutes for vectorization (`paraphrase-multilingual-MiniLM-L12-v2`, 384 dims). It expects `movie_subtitles.s3_path` and `is_vectorized_for_ai` columns and will query `MovieSubtitle.s3_path` to retrieve VTT content (mocked in the repo).
 - **Quota & backoff:** the Java worker now catches `OpenSubtitlesQuotaExceededException`, sets an internal lock and a `resumeAt` timestamp and will pause processing for ~24 hours when the hard daily download limit is reached. Check the worker logs for "Worker paused until" messages.
 - **Schema mismatch warning:** Java schema now has `os_file_id/local_path/is_downloaded` + `translation_status/translated_path/lines_translated` (V23), while `python-ai` models still expect `movie_subtitles.s3_path/format/is_vectorized_for_ai`

### 4. User Authentication (JWT)
- **Files:** `security/JwtAuthenticationFilter.java`, `security/JwtService.java`, `security/RefreshTokenService.java`, `security/CustomUserDetailsService.java`, `user/UserDetailsMapper.java`, `config/JwtProperties.java`, `auth/AuthController.java`
- **Pattern:** Extract token from `Authorization: Bearer <jwt>` header → validate → load UserDetails → set SecurityContext
- **Config:** `app.jwt.secret` (env var `JWT_SECRET`), `app.jwt.access-ttl-min` (default 30), `app.jwt.refresh-ttl-days` (default 30); `V12__security_jwt.sql` adds `users.enabled` and the `refresh_tokens` table
- **Roles:** Stored in `user_roles` table (M2M with User); use `@PreAuthorize("hasRole('ADMIN')")` on restricted endpoints
- **Refresh tokens:** RefreshToken entity + refresh token rotation in auth flow; disabled users are rejected by `CustomUserDetailsService`
- **Swagger auth flow:** OpenAPI config uses OAuth2 password flow with token URL `/auth/swagger-login` (`OpenApiConfig` + hidden endpoint in `AuthController`)

### 5. TMDB ID Enrichment
- **Files:** `client/TmdbClient.java`, `service/KinopoiskSyncService.java`
- **Pattern:** During `KinopoiskSyncService.fetchAndSave(...)`, if Kinopoisk response has no `imdbId`, service calls `TmdbClient.searchMovieId(title, year)` and stores fallback `tmdbId` in `Movie`
- **Config:** `tmdb.api.token` in `application.properties` (env var `TMDB_API_TOKEN`)
- **Usage note:** `tmdbId` is used as fallback identifier for OpenSubtitles search when `imdbId` is missing

## Database Schema Patterns

**Flyway migrations** (`/src/main/resources/db/migration/`):
- **V1__init.sql**: Initial schema (users, movies, genres)
- **V4__movie_genres_m2m.sql**: Many-to-many genres table
- **V5__users_roles.sql**: User roles table
- **V7__ratings_watchlists.sql**: Ratings and watchlist support
- **V8__watch_history.sql**: Watch history tracking
- **V12__security_jwt.sql**: JWT/auth support (`users.enabled`, `refresh_tokens`, seed admin roles)
- **V13__reviews_and_rating_cleanup.sql**: Review system refinements
- **V14__user_profiles.sql**: User profile table
- **V15__email_verification_tokens.sql**: Email verification token table for email-change flows
- **V16__remove_nickname_email_add_bio.sql**: Profile cleanup (`nickname`, `email`, `email_verified` removed; `bio` added)
- **V17__user_settings.sql**: User settings table
- **V18__comments_and_reactions.sql**: Comment threads with reactions
- **V19__add_kinopoisk_id.sql**: Kinopoisk ID column for movies
- **V20__ai_extensions.sql**: Domestic cinema flags/indexes (`is_domestic`, `kz_cultural_weight`)
- **V21__movie_subtitles.sql**: Subtitle queue table (`movie_subtitles`: `os_file_id`, `local_path`, `is_downloaded`)
- **V22__add_tmdb_id.sql**: `movies.tmdb_id` for TMDB/OpenSubtitles fallback
- **V23__subtitle_translation_status.sql**: translation tracking fields in `movie_subtitles` (`translation_status`, `translated_path`, `lines_translated`)

**Key entities & their relationships:**
```
User (1:N) ← WatchHistory, Review, Rating, Watchlist, Comment
User (1:1) ← UserProfile, UserSettings
Movie (1:N) ← Genre (M:M), WatchHistory, Review, Rating
Review (1:N) ← ReviewReaction, Comment
Comment (1:N) ← CommentReaction
```

## Configuration & Startup

**Properties file:** `src/main/resources/application.properties`
- Database: PostgreSQL at `localhost:5432/testdb` (user: `test_user`, pass from env `DB_PASSWORD`; Docker default is `pass1`)
- Kinopoisk API key: env var `KINOPOISK_API_KEY`
- TMDB token: env var `TMDB_API_TOKEN` (`tmdb.api.token`)
- OpenSubtitles credentials: `opensubtitles.api.key`, `opensubtitles.username`, `opensubtitles.password`
- Local subtitle storage: `storage.local.base-path` (default `./storage`)
- Subtitle translator bridge: `ai.translator.url` (default `http://localhost:8100/api/translate`), `ai.translator.secret` (must match Python `INTERNAL_SECRET`)
- JWT secret: env var `JWT_SECRET` (required for production)
- Stream base URL: no longer configured in current `application.properties`; the legacy `StreamController` source is commented out
- Swagger UI: auto-enabled at `http://localhost:8080/swagger-ui.html`

**Docker setup:** Run `docker-compose up` to start PostgreSQL (see `docker-compose.yml`)

**Build & run:**
```bash
./gradlew build         # Build with tests
./gradlew bootRun       # Run app locally
./gradlew test          # Run tests (integration tests in /src/test/java/)
```

**Key tests:** `MovieControllerAddFromKinopoiskTest`, `ReviewSmokeTest`; helper `test/TestAuth.java` is reused for auth bootstrap; `StreamControllerTest.java` is commented out and not part of the active suite

## Common Workflows for Agents

### Adding a New Movie
1. Call `POST /movies/addFromKinopoisk?kinopoiskId=301` (ADMIN role required)
2. `MovieController` → calls `KinopoiskSyncService.fetchAndSave(kinopoiskId)`
3. `KinopoiskClient` makes HTTP call to kinopoisk.dev API
4. Movie metadata + genres cached in `Movie` entity + M2M `movie_genres` table
5. Response returns full `Movie` entity with nested genres

### Fetching & Vectorizing Subtitles
1. Trigger metadata discovery from Java (`POST /api/test/subtitles/discover/{movieId}`) to enqueue one base subtitle (`kk → ru → en`) in `movie_subtitles`
2. Trigger Java downloader (`POST /api/test/subtitles/trigger-worker`) or wait for scheduler if `@EnableScheduling` is enabled later
3. Java worker downloads subtitle via OpenSubtitles, saves file under `./storage/subtitles/{movieId}/{lang}.vtt`, sets `is_downloaded=true` and stores a normalized `local_path` (forward slashes).
4. Before calling the translator, the worker converts `local_path` to an absolute, normalized path and synthesizes an absolute `output_path` that ends with `kk.srt`. `SubtitleTranslationClient.triggerTranslation(...)` sends a JSON body containing `movie_id`, `input_path` (absolute VTT path), `output_path` (absolute kk.srt path), `movie_title`, and `source_language` to `ai.translator.url`.
5. `subtitle-translator` processes queue and calls Java webhook `POST /api/internal/subtitles/translation-complete` with `X-Internal-Secret`
 - Note: the Java worker will pause for ~24 hours when OpenSubtitles hard daily download limits are hit; check worker logs for "Worker paused until" and use the manual trigger endpoint to re-run if necessary.
6. (Optional/independent) Start Python vectorization worker from `/python-ai/` (APScheduler)
7. Python vectorization worker vectorizes subtitle text and writes `SubtitleChunk` embeddings (384 dims)

### User Registration & JWT Flow
1. POST `/auth/register` → create User entity, hash password, set `ROLE_USER`, and create an empty `UserProfile`
2. POST `/auth/login` → validate credentials, generate JWT (access + refresh tokens)
3. Client includes JWT in `Authorization: Bearer <token>` header
4. `JwtAuthenticationFilter` validates token on each request; `/auth/refresh` rotates the stored refresh token and returns a new access token
5. Roles checked via `@PreAuthorize` annotations on controllers

## Conventions & Patterns

### Service Layer
- **Separation:** Service handles business logic; Controller receives HTTP requests and delegates
- **Example:** `MovieService.addMovie()` takes `MovieDto`, builds Movie entity, handles genre assignment
- **No direct DB in controllers:** Prefer services for business logic, but a few simple endpoints still hit repositories directly (for example `MovieController.getMovieById()` and `GenreController.createGenre()`)

### DTO Usage
- Located in `/dto/` and subdirectories (`/dto/comment/`, `/dto/review/`, `/dto/settings/`, `/dto/profile/`, `/dto/rating/`)
- Auth request/response DTOs live under `/auth/dto/` (`LoginRequest`, `RegisterRequest`, `RefreshRequest`, `AuthResponse`)
- Used for most request/response contracts (`review`, `rating`, `comment`, `profile`, `settings`)
- Note: some endpoints (for example `MovieController`) still return JPA entities directly
- Example: `MovieDto` transfers movie data in API contracts

### Async & Scheduled Tasks
- Java module contains `@Scheduled` worker `SubtitleDownloadWorker.processDownloadQueue()`, but `TestCinemaApplication` currently does not enable scheduling (`@EnableScheduling` is absent)
- Translation service (`subtitle-translator`) runs an in-process async queue worker (`worker_loop`) started from FastAPI lifespan; it is not APScheduler-based
- Periodic subtitle vectorization (if used) runs in Python via APScheduler (`python-ai/workers/worker.py`)
- Logging pattern in Java uses SLF4J (`LoggerFactory`) and `log.info()/warn()/error()`

### Exception Handling
- Custom exceptions in `/exception/` package
- Primary pattern is centralized `@ControllerAdvice` in `RestExceptionHandler`
- Services/controllers throw (`IllegalArgumentException`, `ResponseStatusException`, `BusinessException`), handler maps to 4xx/5xx

### Security Annotations
- `@PreAuthorize("hasRole('ADMIN')")`: Restrict to admin users (check `user_roles` table)
- `@PreAuthorize("hasRole('USER')")` or no annotation: Available to authenticated users
- Pattern: JWT extracted by filter, UserDetails loaded, roles checked by Spring Security

## Important Files & Their Purpose

| File | Purpose |
|------|---------|
| `build.gradle` | Gradle dependencies, Java 17, plugins (Flyway, Spring Boot 3.5.6) |
| `src/main/resources/db/migration/V*.sql` | Flyway SQL migrations; always name correctly (V1, V2, V3, ...) |
| `TestCinemaApplication.java` | Entry point; enables `@ConfigurationProperties(JwtProperties)` |
| `config/*.java` | Spring configuration: security, OpenAPI, JPA repository/entity scanning |
| `controller/*.java` | REST endpoints with `@RestController`, `@RequestMapping`, OpenAPI docs |
| `auth/AuthController.java` | JWT auth endpoints (`/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`) |
| `controller/StreamController.java` | Legacy stream resolver scaffold; currently commented out, but `/stream/**` and `/movies/*/stream` remain whitelisted in `SecurityConfig` |
| `client/TmdbClient.java` | TMDB client used to enrich `movies.tmdb_id` when Kinopoisk does not return `imdbId` |
| `client/OpenSubtitlesClient.java` | OpenSubtitles auth/search/download client with quota handling (`406/429`) |
| `security/CustomUserDetailsService.java` + `user/UserDetailsMapper.java` | Loads enabled users by email and maps roles into `UserPrincipal` |
| `service/*.java` | Business logic, API clients, scheduled workers |
| `service/SubtitleMetadataService.java` + `service/SubtitleDownloadWorker.java` | Java subtitle discovery queue + downloader (`movie_subtitles` → local `storage/subtitles`) |
| `client/SubtitleTranslationClient.java` + `controller/SubtitleTranslationWebhookController.java` | Java → Python subtitle translation trigger and secure webhook callback (`/api/internal/subtitles/translation-complete`) |
| `controller/SubtitleTestController.java` | Manual subtitle pipeline triggers for development/testing |
| `security/JwtAuthenticationFilter.java` | Validates JWT tokens on incoming requests |
| `auth/dto/*.java` | Request/response DTOs for the auth flow |
| `model/*.java` | JPA entities with `@Entity`, relationships (`@OneToMany`, `@ManyToMany`), Flyway must match schema |
| `repository/*.java` | Spring Data JPA interfaces; extend `JpaRepository<Entity, ID>` |
| `dto/**/*.java` | DTOs for request/response serialization |
| `python-ai/` | Async AI worker (FastAPI) for subtitle vectorization |
| `subtitle-translator/` | FastAPI microservice for queued subtitle translation + webhook callbacks to Java |
| `exception/RestExceptionHandler.java` | Global HTTP error mapping for validation/security/business errors |
| `src/test/java/com/cinema/testcinema/` | Integration tests (use `@SpringBootTest` + test properties) |

## Testing Strategy

**Location:** `src/test/java/com/cinema/testcinema/`

**Test properties:** `src/test/resources/application-test.properties` (separate DB config for tests)

**Pattern:** Use `@SpringBootTest` with H2 (`MODE=PostgreSQL`) and Flyway (`src/test/resources/application-test.properties`)

**Example tests:** `MovieControllerAddFromKinopoiskTest` covers the Kinopoisk import flow; `ReviewSmokeTest` covers authenticated review creation via JWT helper `TestAuth`. `StreamControllerTest.java` is currently commented out and not part of the active suite.

## Performance & Debugging Tips

1. **Flyway migration stuck?** Check `flyway_schema_history` table; manually delete bad entries if needed
2. **Kinopoisk API rate limited?** Worker catches errors; check logs for "limit exceeded" messages; data cached in DB so subsequent requests fast
3. **Subtitle flow not running?** Java downloader scheduling requires `@EnableScheduling`; otherwise use `POST /api/test/subtitles/trigger-worker`. For translation callback, verify `ai.translator.secret` (Java) equals `INTERNAL_SECRET` (Python). `python-ai` worker still expects columns (`s3_path`, `is_vectorized_for_ai`) not present in Java schema (`V21` + `V23`)
    - Note: the Java worker normalizes `local_path` to an absolute, forward-slash path when calling the translator to avoid cross-platform working-directory issues. If downloads stop unexpectedly, check the worker logs for quota backoff messages ("Worker paused until ...") — the worker will pause for ~24 hours after `OpenSubtitlesQuotaExceededException`.
4. **JWT token invalid?** Ensure `JWT_SECRET` env var set; token must match secret; check expiry (`accessTtlMin`)
5. **Swagger docs not showing?** Visit `http://localhost:8080/swagger-ui.html` after app starts; auto-generated from `@Operation`, `@Parameter` annotations

## AI Agent Productivity Checklist

- [ ] Understand the Movie-Genre M2M relationship (see V4__movie_genres_m2m.sql)
- [ ] Familiarize with JWT flow: extraction → validation → UserDetails loading → role checks
- [ ] Know Flyway naming convention: `V<number>__<description>.sql` (must be sequential)
- [ ] When adding endpoints, use DTOs for requests/responses; never expose JPA entities
- [ ] Check profile/settings modules (`V14`, `V17`, `ProfileController`, `SettingsController`) before touching user-facing features
- [ ] Test locally with `docker-compose up` + `./gradlew bootRun`; run `./gradlew test` before commits
- [ ] Check existing controllers for patterns (e.g., error handling, logging, annotations)
- [ ] Java subtitle pipeline exists (`SubtitleMetadataService`, `SubtitleDownloadWorker`, `SubtitleTestController`), but scheduled execution is effectively off until `@EnableScheduling` is enabled
- [ ] If subtitle translation is used, keep Java `ai.translator.secret` and Python `INTERNAL_SECRET` aligned; webhook updates `movie_subtitles.translation_status/*_path/lines_translated`

---

**Updated:** 2026-05-08 | **Java 17** | **Spring Boot 3.5.6** | **PostgreSQL 16** | **Python 3.10+**
