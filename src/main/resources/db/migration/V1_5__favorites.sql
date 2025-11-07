-- V1_5__favorites.sql
CREATE TABLE user_favorites (
                                user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
                                movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                PRIMARY KEY (user_id, movie_id)
);
CREATE INDEX idx_fav_user ON user_favorites(user_id);
CREATE INDEX idx_fav_movie ON user_favorites(movie_id);
