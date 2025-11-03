-- V4__watch_history.sql
CREATE TABLE watch_history (
                               id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
                               movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                               last_position_sec INT NOT NULL DEFAULT 0,          -- позиция плеера при выходе
                               progress_percent SMALLINT NOT NULL DEFAULT 0,      -- 0..100
                               last_viewed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_hist_user_time ON watch_history(user_id, last_viewed_at DESC);
CREATE UNIQUE INDEX uq_hist_last ON watch_history(user_id, movie_id);
