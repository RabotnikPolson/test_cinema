-- 1) На всякий случай: создать таблицу связей, если её нет
CREATE TABLE IF NOT EXISTS movie_genres (
                                            movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                                            genre_id BIGINT NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
                                            PRIMARY KEY (movie_id, genre_id)
);

-- 2) Перенести существующие связи из movies.genre_id в movie_genres
DO $$
    BEGIN
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'movies' AND column_name = 'genre_id'
        ) THEN
            INSERT INTO movie_genres(movie_id, genre_id)
            SELECT id AS movie_id, genre_id
            FROM movies
            WHERE genre_id IS NOT NULL
            ON CONFLICT DO NOTHING;
        END IF;
    END$$;

-- 3) Снять возможные внешние ключи на movies.genre_id и удалить колонку
DO $$
    DECLARE r RECORD;
    BEGIN
        FOR r IN
            SELECT c.conname
            FROM pg_constraint c
                     JOIN pg_class t ON c.conrelid = t.oid
            WHERE t.relname = 'movies' AND c.conname ILIKE '%genre%'
            LOOP
                EXECUTE format('ALTER TABLE movies DROP CONSTRAINT %I', r.conname);
            END LOOP;

        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'movies' AND column_name = 'genre_id'
        ) THEN
            ALTER TABLE movies DROP COLUMN genre_id;
        END IF;
    END$$;

-- 4) Индексы для быстрого поиска по связке
CREATE INDEX IF NOT EXISTS idx_movie_genres_movie ON movie_genres(movie_id);
CREATE INDEX IF NOT EXISTS idx_movie_genres_genre ON movie_genres(genre_id);
