-- V27: seed 4 realistic test users for demo
-- Passwords: all "demo1234" (BCrypt strength 10)
-- Profiles: kazakhfilm_fan (KZ lover), action_lover (foreign action), cinephile_kz (mixed), new_user (cold start)

-- ─── 1. Users ────────────────────────────────────────────────────────────────
INSERT INTO users (email, username, password_hash, created_at, enabled)
VALUES
    ('kz.fan@cinema.demo',    'kazakhfilm_fan', '$2a$10$PCo0HMurpGz7.AHEqFlYy.ZUynOkIKiKbr.8zATYYYhkdIDmQjquq', now() - INTERVAL '60 days', TRUE),
    ('action@cinema.demo',    'action_lover',   '$2a$10$xQYDXe88T9zRftS9m8uhLuJhcNBSmcrLSLm9xX4rOCpsmwPKZsVn6', now() - INTERVAL '45 days', TRUE),
    ('cinephile@cinema.demo', 'cinephile_kz',   '$2a$10$ejXIRs2ZdjQtrqiG.2YUnuyyIkpqSGaXuWa5fOQkdTGrNAuNICv4W', now() - INTERVAL '30 days', TRUE),
    ('newuser@cinema.demo',   'new_user',        '$2a$10$842Lo8b6FQ/MOxCe8P/eYuP1b2S3CJM2VvyX6ixT9dzxHOxsU930u', now() - INTERVAL '1 day',   TRUE)
ON CONFLICT (email) DO NOTHING;

-- ─── 2. Roles ─────────────────────────────────────────────────────────────────
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.role_name = 'ROLE_USER'
WHERE u.email IN ('kz.fan@cinema.demo', 'action@cinema.demo', 'cinephile@cinema.demo', 'newuser@cinema.demo')
ON CONFLICT DO NOTHING;

-- ─── 3. User Profiles ─────────────────────────────────────────────────────────
INSERT INTO user_profiles (user_id)
SELECT u.id
FROM users u
WHERE u.email IN ('kz.fan@cinema.demo', 'action@cinema.demo', 'cinephile@cinema.demo', 'newuser@cinema.demo')
ON CONFLICT (user_id) DO NOTHING;

-- new_user: no ratings, no watch history — demonstrates cold start fallback to popular/trending

-- ─── 4. Ratings ───────────────────────────────────────────────────────────────
-- kazakhfilm_fan: high scores for KZ domestic films (93,106,84,85,81,111,108,68), low for foreign
INSERT INTO ratings (user_id, movie_id, score, created_at)
SELECT u.id, m.movie_id, m.score, now() - (m.days_ago * INTERVAL '1 day')
FROM users u
CROSS JOIN (VALUES
    (93,  10, 55), (106, 9,  50), (84,  10, 45), (85,  9,  42),
    (81,  8,  38), (111, 10, 35), (108, 9,  30), (68,  8,  25),
    (19,  5,  20), (21,  6,  15)
) AS m(movie_id, score, days_ago)
WHERE u.email = 'kz.fan@cinema.demo'
  AND EXISTS (SELECT 1 FROM movies WHERE id = m.movie_id)
ON CONFLICT (user_id, movie_id) DO NOTHING;

-- action_lover: high scores for foreign action/thriller (20,67,73,3,21,12,61,32), low for KZ
INSERT INTO ratings (user_id, movie_id, score, created_at)
SELECT u.id, m.movie_id, m.score, now() - (m.days_ago * INTERVAL '1 day')
FROM users u
CROSS JOIN (VALUES
    (20,  10, 40), (67,  9,  37), (73,  9,  34), (3,   8,  31),
    (21,  10, 28), (12,  8,  25), (61,  9,  22), (32,  7,  18),
    (93,  4,  15), (84,  3,  12)
) AS m(movie_id, score, days_ago)
WHERE u.email = 'action@cinema.demo'
  AND EXISTS (SELECT 1 FROM movies WHERE id = m.movie_id)
ON CONFLICT (user_id, movie_id) DO NOTHING;

-- cinephile_kz: broad taste — mix of KZ and foreign, moderate-high scores
INSERT INTO ratings (user_id, movie_id, score, created_at)
SELECT u.id, m.movie_id, m.score, now() - (m.days_ago * INTERVAL '1 day')
FROM users u
CROSS JOIN (VALUES
    (93,  8,  50), (106, 7,  48), (85,  9,  45), (20,  8,  42),
    (67,  8,  40), (12,  9,  38), (81,  7,  35), (73,  8,  32),
    (42,  9,  28), (50,  8,  25), (111, 7,  22), (61,  9,  18)
) AS m(movie_id, score, days_ago)
WHERE u.email = 'cinephile@cinema.demo'
  AND EXISTS (SELECT 1 FROM movies WHERE id = m.movie_id)
ON CONFLICT (user_id, movie_id) DO NOTHING;

-- ─── 5. Watch History ─────────────────────────────────────────────────────────
-- kazakhfilm_fan: completed most KZ films, bounced on foreign
INSERT INTO watch_history (user_id, movie_id, session_id, started_at, seconds_watched, completed, last_beat_at)
SELECT u.id, m.movie_id, m.sess::UUID,
       now() - (m.days_ago * INTERVAL '1 day'),
       m.secs, m.done,
       now() - (m.days_ago * INTERVAL '1 day') + (m.secs * INTERVAL '1 second')
FROM users u
CROSS JOIN (VALUES
    (93,  'a0000000-0000-0000-0000-000000000001', 55, 6800, TRUE),
    (106, 'a0000000-0000-0000-0000-000000000002', 50, 7200, TRUE),
    (84,  'a0000000-0000-0000-0000-000000000003', 45, 5800, TRUE),
    (85,  'a0000000-0000-0000-0000-000000000004', 42, 7100, TRUE),
    (81,  'a0000000-0000-0000-0000-000000000005', 38, 6200, TRUE),
    (111, 'a0000000-0000-0000-0000-000000000006', 35, 5400, FALSE),
    (19,  'a0000000-0000-0000-0000-000000000007', 20, 1800, FALSE)
) AS m(movie_id, sess, days_ago, secs, done)
WHERE u.email = 'kz.fan@cinema.demo'
  AND EXISTS (SELECT 1 FROM movies WHERE id = m.movie_id);

-- action_lover: completed foreign action films, bounced on KZ
INSERT INTO watch_history (user_id, movie_id, session_id, started_at, seconds_watched, completed, last_beat_at)
SELECT u.id, m.movie_id, m.sess::UUID,
       now() - (m.days_ago * INTERVAL '1 day'),
       m.secs, m.done,
       now() - (m.days_ago * INTERVAL '1 day') + (m.secs * INTERVAL '1 second')
FROM users u
CROSS JOIN (VALUES
    (20,  'b0000000-0000-0000-0000-000000000001', 40, 8400, TRUE),
    (67,  'b0000000-0000-0000-0000-000000000002', 37, 7600, TRUE),
    (73,  'b0000000-0000-0000-0000-000000000003', 34, 6900, TRUE),
    (3,   'b0000000-0000-0000-0000-000000000004', 31, 8100, TRUE),
    (21,  'b0000000-0000-0000-0000-000000000005', 28, 7800, TRUE),
    (12,  'b0000000-0000-0000-0000-000000000006', 25, 7200, TRUE),
    (93,  'b0000000-0000-0000-0000-000000000007', 15, 900,  FALSE)
) AS m(movie_id, sess, days_ago, secs, done)
WHERE u.email = 'action@cinema.demo'
  AND EXISTS (SELECT 1 FROM movies WHERE id = m.movie_id);

-- cinephile_kz: completed a variety across both KZ and foreign
INSERT INTO watch_history (user_id, movie_id, session_id, started_at, seconds_watched, completed, last_beat_at)
SELECT u.id, m.movie_id, m.sess::UUID,
       now() - (m.days_ago * INTERVAL '1 day'),
       m.secs, m.done,
       now() - (m.days_ago * INTERVAL '1 day') + (m.secs * INTERVAL '1 second')
FROM users u
CROSS JOIN (VALUES
    (93,  'c0000000-0000-0000-0000-000000000001', 50, 6800, TRUE),
    (20,  'c0000000-0000-0000-0000-000000000002', 42, 8200, TRUE),
    (12,  'c0000000-0000-0000-0000-000000000003', 38, 7100, TRUE),
    (85,  'c0000000-0000-0000-0000-000000000004', 35, 6900, TRUE),
    (42,  'c0000000-0000-0000-0000-000000000005', 28, 7300, TRUE),
    (67,  'c0000000-0000-0000-0000-000000000006', 22, 6400, TRUE),
    (50,  'c0000000-0000-0000-0000-000000000007', 18, 5800, FALSE)
) AS m(movie_id, sess, days_ago, secs, done)
WHERE u.email = 'cinephile@cinema.demo'
  AND EXISTS (SELECT 1 FROM movies WHERE id = m.movie_id);

-- ─── 6. Movie Clicks (implicit feedback) ─────────────────────────────────────
INSERT INTO movie_clicks (user_id, movie_id, clicked_at)
SELECT u.id, m.movie_id, now() - (m.days_ago * INTERVAL '1 day')
FROM users u
CROSS JOIN (VALUES
    (93, 56), (106, 52), (84, 47), (85, 44), (81, 40), (111, 37),
    (108, 33), (68, 28), (82, 25), (15, 21), (19, 18), (21, 14)
) AS m(movie_id, days_ago)
WHERE u.email = 'kz.fan@cinema.demo'
  AND EXISTS (SELECT 1 FROM movies WHERE id = m.movie_id);

INSERT INTO movie_clicks (user_id, movie_id, clicked_at)
SELECT u.id, m.movie_id, now() - (m.days_ago * INTERVAL '1 day')
FROM users u
CROSS JOIN (VALUES
    (20, 41), (67, 38), (73, 35), (3, 32), (21, 29), (12, 26),
    (61, 23), (32, 19), (42, 16), (50, 12), (93, 14), (84, 11)
) AS m(movie_id, days_ago)
WHERE u.email = 'action@cinema.demo'
  AND EXISTS (SELECT 1 FROM movies WHERE id = m.movie_id);

INSERT INTO movie_clicks (user_id, movie_id, clicked_at)
SELECT u.id, m.movie_id, now() - (m.days_ago * INTERVAL '1 day')
FROM users u
CROSS JOIN (VALUES
    (93, 51), (106, 49), (85, 46), (20, 43), (67, 40), (12, 37),
    (81, 34), (73, 31), (42, 27), (50, 24), (111, 20), (61, 17)
) AS m(movie_id, days_ago)
WHERE u.email = 'cinephile@cinema.demo'
  AND EXISTS (SELECT 1 FROM movies WHERE id = m.movie_id);