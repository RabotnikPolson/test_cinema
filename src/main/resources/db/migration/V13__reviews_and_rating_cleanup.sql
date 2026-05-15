CREATE TABLE IF NOT EXISTS reviews (
    id BIGSERIAL PRIMARY KEY,
    movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_id BIGINT REFERENCES reviews(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    edited BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_reviews_movie_created ON reviews(movie_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_reviews_parent ON reviews(parent_id);

INSERT INTO reviews(movie_id, user_id, parent_id, content, created_at, updated_at)
SELECT r.movie_id,
       r.user_id,
       NULL,
       r.comment,
       COALESCE(r.created_at, now()),
       COALESCE(r.created_at, now())
FROM ratings r
WHERE r.comment IS NOT NULL AND length(trim(r.comment)) > 0;

ALTER TABLE ratings
    DROP COLUMN IF EXISTS comment;
