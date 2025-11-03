-- V1_3__subscription_billing.sql
CREATE TABLE IF NOT EXISTS subscriptions (
                                             user_id BIGINT PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
                                             plan TEXT NOT NULL,                   -- 'free','pro','team' и т.п.
                                             status TEXT NOT NULL,                 -- 'active','past_due','canceled'
                                             current_period_end TIMESTAMPTZ,
                                             meta JSONB NOT NULL DEFAULT '{}'::jsonb,
                                             updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS payment_methods (
                                               id SERIAL PRIMARY KEY,
                                               user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
                                               brand TEXT,
                                               last4 TEXT,
                                               exp_month INT,
                                               exp_year INT,
                                               is_default BOOLEAN NOT NULL DEFAULT true,
                                               token_ref TEXT,
                                               created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_payment_methods_user ON payment_methods(user_id);
