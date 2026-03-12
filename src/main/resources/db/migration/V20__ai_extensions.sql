-- Активация расширения для векторного семантического поиска
CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE movies
    -- Флаг отечественного кино (Истина для фильмов РК)
    ADD COLUMN IF NOT EXISTS is_domestic BOOLEAN NOT NULL DEFAULT FALSE,
    -- Кинопоиск ID (уже был добавлен в V19, но гарантируем его наличие)
    ADD COLUMN IF NOT EXISTS kinopoisk_id VARCHAR(100),
    -- Культурный вес для ранжирования (например, фильмы про историю РК = +5, обычные = +1)
    ADD COLUMN IF NOT EXISTS kz_cultural_weight INT NOT NULL DEFAULT 1,
    -- Эмбеддинги для семантического AI-поиска (размерность 384 для rubert-tiny2)
    ADD COLUMN IF NOT EXISTS vector_embedding vector(384);

-- Индексы для оптимизации
CREATE INDEX IF NOT EXISTS idx_movies_kinopoisk_id ON movies(kinopoisk_id);
CREATE INDEX IF NOT EXISTS idx_movies_is_domestic ON movies(is_domestic);
-- HNSW индекс для быстрого векторного поиска (ближайшие соседи)
CREATE INDEX IF NOT EXISTS idx_movies_vector ON movies USING hnsw (vector_embedding vector_cosine_ops);
