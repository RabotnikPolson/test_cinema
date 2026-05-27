-- V28: seed realistic click data for recommendation_impressions
-- Simulates user click-through behaviour for demo CTR metrics.
-- Target CTR: because_you_liked ~12%, kazakhstan ~8%, hybrid ~5%, content ~4%, genre ~6%

-- because_you_liked: mark every ~8th impression as clicked (~12% CTR)
UPDATE recommendation_impressions
SET clicked = true,
    clicked_at = shown_at + (INTERVAL '1 second' * (id % 7 + 2))
WHERE strategy = 'because_you_liked'
  AND clicked = false
  AND id % 8 IN (0, 1);

-- kazakhstan: mark every ~12th impression as clicked (~8% CTR)
UPDATE recommendation_impressions
SET clicked = true,
    clicked_at = shown_at + (INTERVAL '1 second' * (id % 5 + 1))
WHERE strategy = 'kazakhstan'
  AND clicked = false
  AND id % 12 IN (0, 1);

-- hybrid: mark every ~20th impression as clicked (~5% CTR)
UPDATE recommendation_impressions
SET clicked = true,
    clicked_at = shown_at + (INTERVAL '1 second' * (id % 4 + 1))
WHERE strategy = 'hybrid'
  AND clicked = false
  AND id % 20 = 0;

-- content: mark every ~25th impression as clicked (~4% CTR)
UPDATE recommendation_impressions
SET clicked = true,
    clicked_at = shown_at + (INTERVAL '1 second' * (id % 6 + 1))
WHERE strategy = 'content'
  AND clicked = false
  AND id % 25 = 0;

-- genre: mark every ~16th impression as clicked (~6% CTR)
UPDATE recommendation_impressions
SET clicked = true,
    clicked_at = shown_at + (INTERVAL '1 second' * (id % 3 + 1))
WHERE strategy = 'genre'
  AND clicked = false
  AND id % 16 = 0;
