-- ─── AI Metrics Tables ───────────────────────────────────────────────────────
-- Используются для обучения модели коллаборативной фильтрации.
-- user_id — nullable, анонимные пользователи пишутся с NULL.
-- movie_id — FK на таблицу movies.

-- 1. Клики по карточке фильма (implicit feedback)
CREATE TABLE movie_clicks (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT,
    guest_session_id VARCHAR(255),
    movie_id         BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    clicked_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_mc_user_id   ON movie_clicks(user_id);
CREATE INDEX idx_mc_movie_id  ON movie_clicks(movie_id);
CREATE INDEX idx_mc_clicked_at ON movie_clicks(clicked_at);
CREATE INDEX idx_mc_guest_session ON movie_clicks(guest_session_id);

-- 2. Поисковые запросы (контентные предпочтения)
CREATE TABLE search_logs (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT,
    guest_session_id VARCHAR(255),
    query            VARCHAR(255) NOT NULL,
    result_count     INT NOT NULL DEFAULT 0,
    searched_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_sl_user_id     ON search_logs(user_id);
CREATE INDEX idx_sl_searched_at ON search_logs(searched_at);
CREATE INDEX idx_sl_guest_session ON search_logs(guest_session_id);

-- 3. События субтитров (определение казахоязычной аудитории)
CREATE TABLE subtitle_events (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT,
    guest_session_id VARCHAR(255),
    movie_id         BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    action           VARCHAR(20) NOT NULL,
    lang             VARCHAR(10),
    created_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_se_user_movie ON subtitle_events(user_id, movie_id);
CREATE INDEX idx_se_guest_session ON subtitle_events(guest_session_id);
