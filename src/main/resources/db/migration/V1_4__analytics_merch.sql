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

CREATE TABLE IF NOT EXISTS merch_products (
                                              id SERIAL PRIMARY KEY,
                                              slug TEXT NOT NULL UNIQUE,
                                              title TEXT NOT NULL,
                                              price_cents INT NOT NULL,
                                              currency TEXT NOT NULL DEFAULT 'USD',
                                              image_url TEXT,
                                              stock INT NOT NULL DEFAULT 0,
                                              active BOOLEAN NOT NULL DEFAULT true,
                                              created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS merch_orders (
                                            id SERIAL PRIMARY KEY,
                                            user_id BIGINT NOT NULL REFERENCES app_users(id),
                                            product_id INT NOT NULL REFERENCES merch_products(id),
                                            qty INT NOT NULL CHECK (qty > 0),
                                            amount_cents INT NOT NULL,
                                            currency TEXT NOT NULL DEFAULT 'USD',
                                            status TEXT NOT NULL DEFAULT 'created', -- created, paid, shipped
                                            created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_merch_orders_user ON merch_orders(user_id);
