CREATE TABLE movie_subtitles (
    id BIGSERIAL PRIMARY KEY,
    movie_id BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    language VARCHAR(10) NOT NULL,
    os_file_id VARCHAR(50) NOT NULL UNIQUE,
    local_path VARCHAR(500),
    is_downloaded BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subtitles_downloaded ON movie_subtitles(is_downloaded);
