CREATE TABLE user_ratings (
                              user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
                              movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                              rating SMALLINT CHECK (rating BETWEEN 1 AND 10),
                              comment TEXT,
                              created_at TIMESTAMPTZ DEFAULT now(),
                              PRIMARY KEY (user_id, movie_id)
);
