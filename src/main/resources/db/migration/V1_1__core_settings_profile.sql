-- V1_1__core_settings_profile.sql
CREATE TABLE IF NOT EXISTS user_settings (
                                             user_id BIGINT PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
                                             data JSONB NOT NULL DEFAULT '{}'::jsonb,
                                             updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_profiles (
                                             user_id BIGINT PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
                                             display_name TEXT NOT NULL,
                                             email TEXT NOT NULL UNIQUE,
                                             avatar_url TEXT,
                                             favorite_genres TEXT[] DEFAULT ARRAY[]::TEXT[],
                                             created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                             updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_profiles_email ON user_profiles(email);
