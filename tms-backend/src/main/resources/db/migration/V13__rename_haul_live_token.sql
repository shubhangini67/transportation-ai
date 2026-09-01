-- Simpler public demo token (HAUL-LIVE → LIVE-DEMO).

UPDATE trips
SET tracking_token = 'LIVE-DEMO'
WHERE tracking_token = 'HAUL-LIVE';
