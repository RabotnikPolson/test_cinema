ALTER TABLE movies ADD COLUMN IF NOT EXISTS video_s3_path VARCHAR(500);

-- Добавляем s3_path для совместимости с python-ai скриптом (чтобы закрыть schema mismatch)
ALTER TABLE movie_subtitles ADD COLUMN IF NOT EXISTS s3_path VARCHAR(500);
