-- src/main/resources/db/migration/V4__drop_genre_id_add_indexes.sql

-- на всякий случай: создать таблицу связей, если вдруг нет
CREATE TABLE IF NOT EXISTS movie_genres (
                                            movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                                            genre_id BIGINT NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
                                            PRIMARY KEY (movie_id, genre_id)
);

-- перенести связи из movies.genre_id (если колонка ещё есть)
DO $$
    BEGIN
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name = 'movies' AND column_name = 'genre_id'
        ) THEN
            INSERT INTO movie_genres(movie_id, genre_id)
            SELECT id, genre_id FROM movies
            WHERE genre_id IS NOT NULL
            ON CONFLICT DO NOTHING;
        END IF;
    END$$;

-- снять все FK на movies.genre_id и удалить колонку
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

-- индексы для m2m
CREATE INDEX IF NOT EXISTS idx_movie_genres_movie ON movie_genres(movie_id);
CREATE INDEX IF NOT EXISTS idx_movie_genres_genre ON movie_genres(genre_id);
