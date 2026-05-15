-- 1) Создать таблицу связей
CREATE TABLE IF NOT EXISTS movie_genres (
                                            movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                                            genre_id BIGINT NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
                                            PRIMARY KEY (movie_id, genre_id)
);

-- 2) Перенести существующие связи из movies.genre_id, если колонка ещё есть
DO $$
    BEGIN
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_name='movies' AND column_name='genre_id'
        ) THEN
            INSERT INTO movie_genres(movie_id, genre_id)
            SELECT id AS movie_id, genre_id
            FROM movies
            WHERE genre_id IS NOT NULL
            ON CONFLICT DO NOTHING;

            -- 3) Удалить внешний ключ и колонку genre_id
            -- имя FK может отличаться, удаляем безопасно
            PERFORM 1
            FROM pg_constraint c
                     JOIN pg_class t ON c.conrelid=t.oid
            WHERE t.relname='movies' AND c.conname LIKE '%genre%';

            -- пытаемся дропауть все похожие ограничения
            DO $inner$
                DECLARE r RECORD;
                BEGIN
                    FOR r IN
                        SELECT c.conname
                        FROM pg_constraint c
                                 JOIN pg_class t ON c.conrelid=t.oid
                        WHERE t.relname='movies' AND c.conname LIKE '%genre%'
                        LOOP
                            EXECUTE format('ALTER TABLE movies DROP CONSTRAINT %I', r.conname);
                        END LOOP;
                END
            $inner$;

            ALTER TABLE movies DROP COLUMN IF EXISTS genre_id;
        END IF;
    END$$;
