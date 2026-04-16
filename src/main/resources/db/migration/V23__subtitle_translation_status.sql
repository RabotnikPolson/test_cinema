ALTER TABLE movie_subtitles ADD COLUMN translation_status VARCHAR(20) DEFAULT 'none';
-- Возможные значения: 'none', 'pending', 'in_progress', 'completed', 'partial', 'failed'
ALTER TABLE movie_subtitles ADD COLUMN translated_path VARCHAR(500);
-- Путь к переведённому файлу (kk.srt)
ALTER TABLE movie_subtitles ADD COLUMN lines_translated INTEGER DEFAULT 0;
