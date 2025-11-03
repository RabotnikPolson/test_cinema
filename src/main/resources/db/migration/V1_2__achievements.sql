-- V1_2__achievements.sql
CREATE TABLE IF NOT EXISTS achievements (
                                            id SERIAL PRIMARY KEY,
                                            code TEXT NOT NULL UNIQUE,
                                            title TEXT NOT NULL,
                                            description TEXT,
                                            meta JSONB NOT NULL DEFAULT '{}'::jsonb,
                                            created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_achievements (
                                                 id SERIAL PRIMARY KEY,
                                                 user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
                                                 achievement_id INT NOT NULL REFERENCES achievements(id) ON DELETE CASCADE,
                                                 progress JSONB NOT NULL DEFAULT '{}'::jsonb,
                                                 earned_at TIMESTAMPTZ,
                                                 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                 UNIQUE(user_id, achievement_id)
);
CREATE INDEX IF NOT EXISTS idx_user_achievements_user ON user_achievements(user_id);
