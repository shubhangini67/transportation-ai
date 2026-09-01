-- Stable public tracking tokens for the Haulmind login demo.

UPDATE trips t
SET tracking_token = 'LANE-DEMO'
FROM (
    SELECT id FROM trips
    WHERE status = 'IN_PROGRESS'
    ORDER BY start_time NULLS LAST, id
    LIMIT 1
) s
WHERE t.id = s.id;

UPDATE trips t
SET tracking_token = 'HAUL-LIVE'
FROM (
    SELECT id FROM trips
    WHERE status = 'IN_PROGRESS'
      AND tracking_token IS DISTINCT FROM 'LANE-DEMO'
    ORDER BY start_time NULLS LAST, id
    LIMIT 1
) s
WHERE t.id = s.id;
