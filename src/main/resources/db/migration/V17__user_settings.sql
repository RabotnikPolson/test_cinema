CREATE TABLE IF NOT EXISTS user_settings (
    user_id BIGINT PRIMARY KEY,
    theme VARCHAR(50) NOT NULL DEFAULT 'LIGHT',
    language VARCHAR(10) NOT NULL DEFAULT 'ru',
    last_email_edit_at TIMESTAMP,
    CONSTRAINT fk_user_settings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
