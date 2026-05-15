CREATE TABLE IF NOT EXISTS ratings (
                                       id BIGSERIAL PRIMARY KEY,
                                       user_id  BIGINT REFERENCES users(id) ON DELETE SET NULL,
                                       movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                                       score SMALLINT NOT NULL CHECK (score BETWEEN 1 AND 10),
                                       comment TEXT,
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
