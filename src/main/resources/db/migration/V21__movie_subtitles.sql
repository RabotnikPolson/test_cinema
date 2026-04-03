CREATE TABLE movie_subtitles (
    id BIGSERIAL PRIMARY KEY,
    movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    
    -- Идентификаторы оригинального файла
    original_language VARCHAR(10) NOT NULL, -- Язык оригинала ('en', 'ru')
    os_file_id VARCHAR(50) NOT NULL UNIQUE, -- ID файла в OpenSubtitles
    os_subtitle_id VARCHAR(50), 
    format VARCHAR(10) NOT NULL,            -- Формат ('srt', 'vtt')
    
    -- Локальное хранение
    storage_path VARCHAR(500),              -- Путь к сырому файлу на диске
    
    -- Флаги состояний (State Machine для пайплайна)
    is_downloaded BOOLEAN DEFAULT FALSE NOT NULL,         -- Скачан ли сырой файл (Java Worker)
    needs_translation BOOLEAN DEFAULT FALSE NOT NULL,     -- Требуется ли перевод (Python llm-subtrans)
    target_language VARCHAR(10),                          -- Целевой язык перевода (обычно 'kk')
    is_vectorized_for_ai BOOLEAN DEFAULT FALSE NOT NULL,  -- Векторизован ли финальный txt/vtt (Python RAG)
    
    -- Аудит
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Индексы для быстрого поиска фильмов
CREATE INDEX idx_movie_subtitles_movie_id ON movie_subtitles(movie_id);

-- ЧАСТИЧНЫЕ ИНДЕКСЫ (Partial Indexes) для мгновенной выборки задач воркерами
-- 1. Для Java SubtitleDownloadWorker (Что нужно скачать сегодня?)
CREATE INDEX idx_subtitles_to_download 
    ON movie_subtitles(created_at) 
    WHERE is_downloaded = FALSE;

-- 2. Для будущего Python llm-subtrans (Что нужно перевести на казахский?)
CREATE INDEX idx_subtitles_to_translate 
    ON movie_subtitles(created_at) 
    WHERE is_downloaded = TRUE AND needs_translation = TRUE;

-- 3. Для будущего Python RAG Worker (Что нужно векторизовать?)
CREATE INDEX idx_subtitles_to_vectorize 
    ON movie_subtitles(created_at) 
    WHERE is_downloaded = TRUE AND needs_translation = FALSE AND is_vectorized_for_ai = FALSE;
