-- V20__reviews_comments_min.sql

-- 1) Отзывы: один текстовый отзыв на фильм от пользователя
CREATE TABLE reviews (
                         id         BIGSERIAL PRIMARY KEY,
                         movie_id   BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                         user_id    BIGINT NOT NULL REFERENCES app_users(id)  ON DELETE CASCADE,
                         body       TEXT   NOT NULL,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         UNIQUE(movie_id, user_id)
);
CREATE INDEX idx_reviews_movie_created ON reviews(movie_id, created_at DESC);
CREATE INDEX idx_reviews_user ON reviews(user_id);

-- 2) Комментарии: к фильму или к конкретному отзыву. Древо по parent_id.
CREATE TABLE comments (
                          id         BIGSERIAL PRIMARY KEY,
                          movie_id   BIGINT NOT NULL REFERENCES movies(id)   ON DELETE CASCADE,
                          review_id  BIGINT     REFERENCES reviews(id)       ON DELETE CASCADE,
                          parent_id  BIGINT     REFERENCES comments(id)      ON DELETE CASCADE,
                          user_id    BIGINT NOT NULL REFERENCES app_users(id)    ON DELETE CASCADE,
                          body       TEXT   NOT NULL,
                          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_comments_movie_created  ON comments(movie_id, created_at DESC);
CREATE INDEX idx_comments_review_created ON comments(review_id, created_at DESC);
CREATE INDEX idx_comments_parent         ON comments(parent_id);

-- 3) Реакции на комментарии: 6 типов, по одной реакции данного типа от пользователя на комментарий
-- Маппинг: 1=❤️, 2=👍, 3=👎, 4=🔥, 5=😂, 6=😢
CREATE TABLE comment_reactions (
                                   comment_id BIGINT  NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
                                   user_id    BIGINT  NOT NULL REFERENCES app_users(id)    ON DELETE CASCADE,
                                   reaction   SMALLINT NOT NULL CHECK (reaction IN (1,2,3,4,5,6)),
                                   created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                   PRIMARY KEY (comment_id, user_id, reaction)
);

-- Триггеры не нужны: счётчики считаем агрегатами при выборке.
