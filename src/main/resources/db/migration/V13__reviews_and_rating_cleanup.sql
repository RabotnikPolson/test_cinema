-- Создание таблицы отзывов (если ещё не создана)
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

-- Индекс по фильму и дате создания (для выборки комментариев к фильму)
CREATE INDEX IF NOT EXISTS idx_reviews_movie_created
    ON reviews(movie_id, created_at DESC);

-- Индекс по parent_id (для дерева/тредов)
CREATE INDEX IF NOT EXISTS idx_reviews_parent
    ON reviews(parent_id);

-- Если в ratings ещё вдруг осталась колонка comment — аккуратно убрать
ALTER TABLE ratings
    DROP COLUMN IF EXISTS comment;
