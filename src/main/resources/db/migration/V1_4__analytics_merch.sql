-- V1_4__analytics_merch.sql
CREATE TABLE IF NOT EXISTS analytics_daily (
                                               id SERIAL PRIMARY KEY,
                                               user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
                                               d DATE NOT NULL,
                                               plays INT NOT NULL DEFAULT 0,
                                               likes INT NOT NULL DEFAULT 0,
                                               shares INT NOT NULL DEFAULT 0,
                                               UNIQUE(user_id, d)
);
CREATE INDEX IF NOT EXISTS idx_analytics_user ON analytics_daily(user_id);

