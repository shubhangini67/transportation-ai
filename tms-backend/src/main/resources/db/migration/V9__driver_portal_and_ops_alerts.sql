-- Link login emails to driver records so the Driver Console can load assigned trips.
UPDATE drivers SET email = 'driver1@tms.com' WHERE email = 'ramesh@tms.com';
UPDATE drivers SET email = 'driver2@tms.com' WHERE email = 'kavita@tms.com';
UPDATE drivers SET email = 'driver3@tms.com' WHERE email = 'raj@tms.com';
UPDATE drivers SET email = 'driver4@tms.com' WHERE email = 'vikram@tms.com';

-- Seed visible operations exceptions (overdue start + delayed in-progress).
UPDATE trips
SET start_time = NOW() - INTERVAL '6 hours',
    end_time = NOW() + INTERVAL '2 hours'
WHERE id = 'e0000000-0000-0000-0000-000000000001'
  AND status = 'PLANNED';

UPDATE trips
SET start_time = NOW() - INTERVAL '10 hours',
    end_time = NOW() + INTERVAL '1 hour'
WHERE id = 'e0000000-0000-0000-0000-000000000006'
  AND status = 'IN_PROGRESS';
