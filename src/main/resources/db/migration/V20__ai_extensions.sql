-- V20: AI Extensions — базовые колонки для приоритизации казахского кино
-- Примечание: pgvector (vector_embedding) добавляется в V21 после установки расширения

ALTER TABLE movies
    -- Флаг отечественного кино (Истина для фильмов РК)
    ADD COLUMN IF NOT EXISTS is_domestic BOOLEAN NOT NULL DEFAULT FALSE,
    -- Культурный вес для ранжирования (фильмы про историю РК = +5, обычные = +1)
    ADD COLUMN IF NOT EXISTS kz_cultural_weight INT NOT NULL DEFAULT 1;

-- Индекс для быстрой фильтрации отечественного кино
CREATE INDEX IF NOT EXISTS idx_movies_is_domestic ON movies(is_domestic);

-- kinopoisk_id уже должен существовать из предыдущей миграции V19
-- Добавляем индекс на случай его отсутствия
CREATE INDEX IF NOT EXISTS idx_movies_kinopoisk_id ON movies(kinopoisk_id);
