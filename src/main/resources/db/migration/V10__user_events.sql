CREATE TABLE user_events (
                             id BIGSERIAL PRIMARY KEY,
                             user_id BIGINT REFERENCES app_users(id) ON DELETE SET NULL,
                             event_type VARCHAR(50),           -- VIEW_MOVIE, ADD_FAVORITE, REMOVE_FAVORITE, PLAY_START, etc
                             movie_id BIGINT REFERENCES movies(id) ON DELETE SET NULL,
                             metadata JSONB,
                             created_at TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_user_events_type_time ON user_events(event_type, created_at DESC);
