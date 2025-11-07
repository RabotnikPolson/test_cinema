-- V5__watch_sessions.sql
CREATE TABLE watch_sessions (
                                id UUID PRIMARY KEY,
                                user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
                                movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                                started_at TIMESTAMPTZ NOT NULL,
                                ended_at TIMESTAMPTZ,                               -- null = ещё идёт
                                total_seconds INT NOT NULL DEFAULT 0,
                                device VARCHAR(64),                                 -- optional
                                client_ip INET                                      -- optional
);
CREATE INDEX idx_ws_user_movie ON watch_sessions(user_id, movie_id);

CREATE TABLE watch_heartbeats (
                                  session_id UUID NOT NULL REFERENCES watch_sessions(id) ON DELETE CASCADE,
                                  ts TIMESTAMPTZ NOT NULL,
                                  pos_sec INT,                                        -- позиция плеера, если есть
                                  PRIMARY KEY (session_id, ts)
);
