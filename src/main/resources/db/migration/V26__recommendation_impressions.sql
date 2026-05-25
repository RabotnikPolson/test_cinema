CREATE TABLE recommendation_impressions (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT,
    movie_id    BIGINT NOT NULL,
    strategy    VARCHAR(32) NOT NULL,
    position    INT NOT NULL,
    shown_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    clicked     BOOLEAN NOT NULL DEFAULT FALSE,
    clicked_at  TIMESTAMPTZ
);

CREATE INDEX idx_ri_user_strategy ON recommendation_impressions(user_id, strategy);
CREATE INDEX idx_ri_movie         ON recommendation_impressions(movie_id);
CREATE INDEX idx_ri_shown_at      ON recommendation_impressions(shown_at);