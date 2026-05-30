-- V29: fresh movie_clicks for seed users within the 10-day trending window
-- V27 clicks are 11-56 days old — outside the window. These add recent signals.

INSERT INTO movie_clicks (user_id, movie_id, clicked_at)
SELECT u.id, m.movie_id, now() - (m.days_ago * INTERVAL '1 day')
FROM users u
CROSS JOIN (VALUES
    (93,  0), (106, 1), (84,  1), (85,  2), (81,  2), (111, 3),
    (108, 3), (68,  4), (82,  5), (19,  6), (21,  7), (15,  8)
) AS m(movie_id, days_ago)
WHERE u.email = 'kz.fan@cinema.demo'
  AND EXISTS (SELECT 1 FROM movies WHERE id = m.movie_id);

INSERT INTO movie_clicks (user_id, movie_id, clicked_at)
SELECT u.id, m.movie_id, now() - (m.days_ago * INTERVAL '1 day')
FROM users u
CROSS JOIN (VALUES
    (20,  0), (67,  0), (73,  1), (3,   2), (21,  3), (12,  4),
    (61,  4), (32,  5), (42,  6), (50,  7), (93,  8), (84,  9)
) AS m(movie_id, days_ago)
WHERE u.email = 'action@cinema.demo'
  AND EXISTS (SELECT 1 FROM movies WHERE id = m.movie_id);

INSERT INTO movie_clicks (user_id, movie_id, clicked_at)
SELECT u.id, m.movie_id, now() - (m.days_ago * INTERVAL '1 day')
FROM users u
CROSS JOIN (VALUES
    (93,  0), (20,  1), (85,  1), (12,  2), (67,  3), (42,  3),
    (81,  4), (73,  5), (50,  6), (106, 7), (111, 8), (61,  9)
) AS m(movie_id, days_ago)
WHERE u.email = 'cinephile@cinema.demo'
  AND EXISTS (SELECT 1 FROM movies WHERE id = m.movie_id);