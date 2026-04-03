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

**Critical add-on:** Async Python AI worker (`/python-ai/`) for subtitle vectorization using `sentence-transformers`; currently standalone from Java request flow.

## Key Integration Points

### 1. Kinopoisk API Integration
- **Files:** `KinopoiskClient.java`, `KinopoiskSyncService.java`, `MovieController.addFromKinopoisk()`
- **Pattern:** Fetch movie metadata by ID → cache in Movie entity → sync genres via M2M table `movie_genres`
- **Key field:** `kinopoiskId` (Kinopoisk ID), `kinopoiskHdId` (alternative ID)
- **Rate limits:** 500 req/day, 20 req/sec (FREE tier from kinopoisk.dev)
- **Caching:** All movie data persisted in DB after first API call; subsequent requests return cached data

### 2. Stream Embed Integration
- **Files:** `StreamController.java`, `application.properties` (`vidsrc.base-url`)
- **Pattern:** Resolve movie by internal ID → if `kinopoiskId` exists return `https://vbdkv.com/api/short/{kinopoiskId}` → otherwise build `vidsrc` embed URL by `imdbId`
- **Validation:** Optional params `ds_lang`, `autoplay`, `sub_url` are validated in controller before URL construction
- **Security:** `/stream/**` is public in `SecurityConfig`

### 3. Subtitle Vectorization Pipeline (Python → Java Bridge)
- **Java side:** No active subtitle queueing pipeline in `src/main/java` (no `SubtitleDownloadWorker`/`SubtitleSyncService` classes)
- **Python side:** `workers/worker.py` polls DB, parses VTT, vectorizes with `paraphrase-multilingual-MiniLM-L12-v2` (384 dims)
- **Vectorizer:** `services/vectorizer.py` handles WebVTT parsing (removes timestamps, tags, converts text → embeddings)
- **Current behavior:** Worker uses `_mock_download_from_s3(...)` stub and schedules job via APScheduler every 5 minutes
- **Dependencies:** `/python-ai/requirements.txt` includes `fastapi`, `asyncpg`, `pgvector`, `sentence-transformers`, `APScheduler`

### 4. User Authentication (JWT)
- **Files:** `JwtAuthenticationFilter.java`, `JwtService.java`, `JwtProperties.java` (config)
- **Pattern:** Extract token from `Authorization: Bearer <jwt>` header → validate → load UserDetails → set SecurityContext
- **Config:** `app.jwt.secret` (env var `JWT_SECRET`), `app.jwt.access-ttl-min` (default 30), `app.jwt.refresh-ttl-days` (default 30)
- **Roles:** Stored in `user_roles` table (M2M with User); use `@PreAuthorize("hasRole('ADMIN')")` on restricted endpoints
- **Refresh tokens:** RefreshToken entity + refresh token rotation in auth flow

## Database Schema Patterns

**Flyway migrations** (`/src/main/resources/db/migration/`):
- **V1__init.sql**: Initial schema (users, movies, genres)
- **V4__movie_genres_m2m.sql**: Many-to-many genres table
- **V5__users_roles.sql**: User roles table
- **V7__ratings_watchlists.sql**: Ratings and watchlist support
- **V8__watch_history.sql**: Watch history tracking
- **V13__reviews_and_rating_cleanup.sql**: Review system refinements
- **V14__user_profiles.sql**: User profile table
- **V17__user_settings.sql**: User settings table
- **V18__comments_and_reactions.sql**: Comment threads with reactions
- **V19__add_kinopoisk_id.sql**: Kinopoisk ID column for movies
- **V20__ai_extensions.sql**: Domestic cinema flags/indexes (`is_domestic`, `kz_cultural_weight`)

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
- Database: PostgreSQL at `localhost:5432/testdb` (user: `test_user`, pass: `pass1`)
- Kinopoisk API key: env var `KINOPOISK_API_KEY` (example key provided in props)
- JWT secret: env var `JWT_SECRET` (required for production)
- Stream base URL: `vidsrc.base-url` (used by `StreamController`)
- Swagger UI: auto-enabled at `http://localhost:8080/swagger-ui.html`

**Docker setup:** Run `docker-compose up` to start PostgreSQL (see `docker-compose.yml`)

**Build & run:**
```bash
./gradlew build         # Build with tests
./gradlew bootRun       # Run app locally
./gradlew test          # Run tests (integration tests in /src/test/java/)
```

**Key tests:** `MovieControllerAddFromKinopoiskTest`, `StreamControllerTest`, `ReviewSmokeTest`

## Common Workflows for Agents

### Adding a New Movie
1. Call `POST /movies/addFromKinopoisk?kinopoiskId=301` (ADMIN role required)
2. `MovieController` → calls `KinopoiskSyncService.fetchAndSave(kinopoiskId)`
3. `KinopoiskClient` makes HTTP call to kinopoisk.dev API
4. Movie metadata + genres cached in `Movie` entity + M2M `movie_genres` table
5. Response returns full `Movie` entity with nested genres

### Fetching & Vectorizing Subtitles
1. Start Python worker from `/python-ai/` (APScheduler)
2. Worker selects subtitles where `is_downloaded=true` and `is_vectorized_for_ai=false`
3. Worker currently uses `_mock_download_from_s3(...)` test stub for VTT content
4. `vectorizer.py` parses VTT and chunks text
5. Embeddings (384-dim) are generated and saved as `SubtitleChunk` records

### User Registration & JWT Flow
1. POST `/auth/register` → create User entity, hash password, set roles
2. POST `/auth/login` → validate credentials, generate JWT (access + refresh tokens)
3. Client includes JWT in `Authorization: Bearer <token>` header
4. `JwtAuthenticationFilter` validates token on each request; if expired, client calls `/auth/refresh` to get new access token
5. Roles checked via `@PreAuthorize` annotations on controllers

## Conventions & Patterns

### Service Layer
- **Separation:** Service handles business logic; Controller receives HTTP requests and delegates
- **Example:** `MovieService.addMovie()` takes `MovieDto`, builds Movie entity, handles genre assignment
- **No direct DB in controllers:** Always use @Service beans for repository access

### DTO Usage
- Located in `/dto/` and subdirectories (`/dto/comment/`, `/dto/review/`, `/dto/settings/`, `/dto/profile/`, `/dto/rating/`)
- Used for most request/response contracts (`review`, `rating`, `comment`, `profile`, `settings`)
- Note: some endpoints (for example `MovieController`) still return JPA entities directly
- Example: `MovieDto` transfers movie data in API contracts

### Async & Scheduled Tasks
- Java module currently has no `@Scheduled` workers and no `@EnableAsync` in `TestCinemaApplication`
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
| `controller/*.java` | REST endpoints with `@RestController`, `@RequestMapping`, OpenAPI docs |
| `auth/AuthController.java` | JWT auth endpoints (`/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`) |
| `controller/StreamController.java` | Public stream URL resolver (`/stream/{id}`) for `kinopoiskId`/`imdbId` |
| `service/*.java` | Business logic, API clients, scheduled workers |
| `security/JwtAuthenticationFilter.java` | Validates JWT tokens on incoming requests |
| `model/*.java` | JPA entities with `@Entity`, relationships (`@OneToMany`, `@ManyToMany`), Flyway must match schema |
| `repository/*.java` | Spring Data JPA interfaces; extend `JpaRepository<Entity, ID>` |
| `dto/**/*.java` | DTOs for request/response serialization |
| `python-ai/` | Async AI worker (FastAPI) for subtitle vectorization |
| `exception/RestExceptionHandler.java` | Global HTTP error mapping for validation/security/business errors |
| `src/test/java/com/cinema/testcinema/` | Integration tests (use `@SpringBootTest` + test properties) |

## Testing Strategy

**Location:** `src/test/java/com/cinema/testcinema/`

**Test properties:** `src/test/resources/application-test.properties` (separate DB config for tests)

**Pattern:** Use `@SpringBootTest` with H2 (`MODE=PostgreSQL`) and Flyway (`src/test/resources/application-test.properties`)

**Example test:** `MovieControllerAddFromKinopoiskTest` tests the full flow: POST /addFromKinopoisk → verify Movie persisted → check genres synced

## Performance & Debugging Tips

1. **Flyway migration stuck?** Check `flyway_schema_history` table; manually delete bad entries if needed
2. **Kinopoisk API rate limited?** Worker catches errors; check logs for "limit exceeded" messages; data cached in DB so subsequent requests fast
3. **Subtitle vectorization not running?** Current Python worker uses mocked download (`_mock_download_from_s3`) and expects DB rows with `is_downloaded=true`
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
- [ ] Python AI worker runs in separate process; Java subtitle queueing pipeline is not present in current module

---

**Updated:** 2026-03-31 | **Java 17** | **Spring Boot 3.5.6** | **PostgreSQL 16** | **Python 3.10+**

