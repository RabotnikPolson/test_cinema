CREATE TABLE IF NOT EXISTS movie_streams (
                                             id          BIGSERIAL PRIMARY KEY,
                                             imdb_id     VARCHAR(32) NOT NULL UNIQUE,
                                             stream_path TEXT        NOT NULL,
                                             created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
