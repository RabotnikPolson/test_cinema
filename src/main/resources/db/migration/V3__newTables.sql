-- V3__mvp_expansion.sql
-- Расширение под похожий поиск
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ===== 1) Доработка существующей таблицы movies (из V1) =====
ALTER TABLE movies
    ADD COLUMN IF NOT EXISTS original_title VARCHAR(255),
    ADD COLUMN IF NOT EXISTS runtime_min    INT,
    ADD COLUMN IF NOT EXISTS plot           TEXT,
    ADD COLUMN IF NOT EXISTS external_id    VARCHAR(100),
    ADD COLUMN IF NOT EXISTS source         VARCHAR(40),
    ADD COLUMN IF NOT EXISTS created_at     TIMESTAMP NOT NULL DEFAULT now();

-- Ограничения на значения
DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'chk_movies_year_range'
        ) THEN
            ALTER TABLE movies
                ADD CONSTRAINT chk_movies_year_range
                    CHECK (year IS NULL OR year BETWEEN 1888 AND 2100);
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'chk_movies_runtime_min'
        ) THEN
            ALTER TABLE movies
                ADD CONSTRAINT chk_movies_runtime_min
                    CHECK (runtime_min IS NULL OR runtime_min BETWEEN 1 AND 2000);
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'uq_movies_external_source'
        ) THEN
            ALTER TABLE movies
                ADD CONSTRAINT uq_movies_external_source
                    UNIQUE (external_id, source);
        END IF;
    END$$;

-- Индексы по названию и году
CREATE INDEX IF NOT EXISTS idx_movies_title_trgm
    ON movies USING GIN (title gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_movies_year
    ON movies(year);

-- Перенос данных из старых колонок V1
UPDATE movies
SET plot = COALESCE(plot, description)
WHERE description IS NOT NULL;

UPDATE movies
SET external_id = imdb_id,
    source      = COALESCE(source, 'omdb')
WHERE imdb_id IS NOT NULL
  AND external_id IS DISTINCT FROM imdb_id;

-- ===== 2) Каталог: жанры и связь многие-ко-многим =====
-- genres уже есть из V1; создаём m2m-таблицу и переносим существующие связи
CREATE TABLE IF NOT EXISTS movie_genres (
                                            movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                                            genre_id BIGINT NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
                                            PRIMARY KEY (movie_id, genre_id)
);

INSERT INTO movie_genres (movie_id, genre_id)
SELECT id, genre_id FROM movies WHERE genre_id IS NOT NULL
ON CONFLICT DO NOTHING;

-- (опционально, после миграции можно удалить старый столбец)
-- ALTER TABLE movies DROP COLUMN IF EXISTS genre_id;

-- ===== 3) Пользователи и роли =====
-- У вас в V1 была app_users. Здесь — новая целевая модель users/roles/user_roles.
CREATE TABLE IF NOT EXISTS users (
                                     id            BIGSERIAL PRIMARY KEY,
                                     email         VARCHAR(255) UNIQUE,
                                     username      VARCHAR(50)  UNIQUE NOT NULL,
                                     password_hash VARCHAR(255) NOT NULL,
                                     created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS roles (
                                     id        BIGSERIAL PRIMARY KEY,
                                     role_name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                          role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                                          PRIMARY KEY (user_id, role_id)
);

-- Перенос из app_users → users/roles (если app_users существует)
DO $$
    BEGIN
        IF EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_name='app_users' AND table_schema='public') THEN

            -- роли из app_users.role
            INSERT INTO roles(role_name)
            SELECT DISTINCT role FROM app_users
            ON CONFLICT (role_name) DO NOTHING;

            -- пользователи
            INSERT INTO users(username, password_hash, email, created_at)
            SELECT au.username, au.password_hash, NULL, now()
            FROM app_users au
            ON CONFLICT (username) DO NOTHING;

            -- связывание пользователей и ролей
            INSERT INTO user_roles(user_id, role_id)
            SELECT u.id, r.id
            FROM app_users au
                     JOIN users u ON u.username = au.username
                     JOIN roles r ON r.role_name = au.role
            ON CONFLICT DO NOTHING;
        END IF;
    END$$;

-- ===== 4) Оценки, избранное =====
CREATE TABLE IF NOT EXISTS ratings (
                                       id         BIGSERIAL PRIMARY KEY,
                                       user_id    BIGINT REFERENCES users(id) ON DELETE SET NULL,
                                       movie_id   BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                                       score      SMALLINT NOT NULL CHECK (score BETWEEN 1 AND 10),
                                       comment    TEXT,
                                       created_at TIMESTAMP NOT NULL DEFAULT now(),
                                       UNIQUE (user_id, movie_id)
);
CREATE INDEX IF NOT EXISTS idx_ratings_movie ON ratings(movie_id);
CREATE INDEX IF NOT EXISTS idx_ratings_user  ON ratings(user_id);

CREATE TABLE IF NOT EXISTS watchlists (
                                          user_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                          movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                                          added_at TIMESTAMP NOT NULL DEFAULT now(),
                                          PRIMARY KEY (user_id, movie_id)
);

-- ===== 5) Видео и субтитры =====
CREATE TABLE IF NOT EXISTS videos (
                                      id        BIGSERIAL PRIMARY KEY,
                                      movie_id  BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                                      kind      VARCHAR(20) NOT NULL CHECK (kind IN ('trailer','clip','cc-film')),
                                      provider  VARCHAR(50) NOT NULL CHECK (provider IN ('youtube','vimeo','cc-host')),
                                      embed_url VARCHAR(500) NOT NULL,
                                      created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_videos_movie ON videos(movie_id);

CREATE TABLE IF NOT EXISTS subtitles (
                                         id         BIGSERIAL PRIMARY KEY,
                                         movie_id   BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                                         lang_code  VARCHAR(8)  NOT NULL,
                                         format     VARCHAR(10) NOT NULL CHECK (format IN ('srt','vtt')),
                                         source_url VARCHAR(500) NOT NULL,
                                         license    VARCHAR(40)  NOT NULL CHECK (license IN ('CC-BY','permission','unknown')),
                                         created_at TIMESTAMP NOT NULL DEFAULT now(),
                                         UNIQUE (movie_id, lang_code)
);
CREATE INDEX IF NOT EXISTS idx_subtitles_movie_lang ON subtitles(movie_id, lang_code);

-- ===== 6) Просмотры и аналитика =====
CREATE TABLE IF NOT EXISTS watch_history (
                                             id              BIGSERIAL PRIMARY KEY,
                                             user_id         BIGINT REFERENCES users(id) ON DELETE SET NULL,
                                             movie_id        BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                                             session_id      UUID   NOT NULL,
                                             started_at      TIMESTAMP NOT NULL,
                                             seconds_watched INT NOT NULL CHECK (seconds_watched >= 0),
                                             completed       BOOLEAN NOT NULL DEFAULT false,
                                             last_beat_at    TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_wh_user_time  ON watch_history(user_id, started_at);
CREATE INDEX IF NOT EXISTS idx_wh_movie_time ON watch_history(movie_id, started_at);
CREATE INDEX IF NOT EXISTS idx_wh_session    ON watch_history(session_id);

-- ===== 7) Импорт (админ) =====
CREATE TABLE IF NOT EXISTS import_jobs (
                                           id          BIGSERIAL PRIMARY KEY,
                                           job_type    VARCHAR(40)  NOT NULL,
                                           status      VARCHAR(20)  NOT NULL CHECK (status IN ('queued','running','failed','completed')),
                                           created_at  TIMESTAMP NOT NULL DEFAULT now(),
                                           finished_at TIMESTAMP,
                                           stats_json  JSONB,
                                           error_text  TEXT
);
