ALTER TABLE users
    ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires ON refresh_tokens(expires_at);

INSERT INTO roles (role_name)
SELECT role_name FROM (VALUES ('ROLE_USER'), ('ROLE_ADMIN')) AS t(role_name)
ON CONFLICT (role_name) DO NOTHING;

INSERT INTO users (email, username, password_hash, created_at, enabled)
VALUES ('admin@cinema.local', 'admin', '$2a$10$7EqJtq98hPqEX7fNZaFWoO5X6lPsAXH/1CajWnUPmZ.9c/.2Wn2da', now(), TRUE)
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
         JOIN roles r ON r.role_name = 'ROLE_ADMIN'
WHERE u.email = 'admin@cinema.local'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
         JOIN roles r ON r.role_name = 'ROLE_USER'
WHERE u.email = 'admin@cinema.local'
ON CONFLICT DO NOTHING;
