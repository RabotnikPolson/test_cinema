ALTER TABLE user_profiles
    DROP COLUMN IF EXISTS nickname,
    DROP COLUMN IF EXISTS email,
    DROP COLUMN IF EXISTS email_verified;

ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS bio TEXT;
