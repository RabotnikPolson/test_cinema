CREATE TABLE playlists (
                           id BIGSERIAL PRIMARY KEY,
                           user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
                           name VARCHAR(100) NOT NULL,
                           created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE playlist_items (
                                playlist_id BIGINT NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
                                movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                                position INT DEFAULT 0,
                                PRIMARY KEY (playlist_id, movie_id)
);
