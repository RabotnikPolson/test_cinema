CREATE TABLE IF NOT EXISTS watch_history (
                                             id BIGSERIAL PRIMARY KEY,
                                             user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
                                             movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                                             session_id UUID NOT NULL,
                                             started_at TIMESTAMP NOT NULL,
                                             seconds_watched INT NOT NULL CHECK (seconds_watched >= 0),
                                             completed BOOLEAN NOT NULL DEFAULT false,
                                             last_beat_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_wh_user_time   ON watch_history(user_id, started_at);
CREATE INDEX IF NOT EXISTS idx_wh_movie_time  ON watch_history(movie_id, started_at);
CREATE INDEX IF NOT EXISTS idx_wh_session     ON watch_history(session_id);
