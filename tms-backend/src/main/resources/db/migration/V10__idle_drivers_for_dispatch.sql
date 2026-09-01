-- Extra idle drivers so Smart Dispatch has pairs while several units are already on trips.
UPDATE drivers SET status = 'ACTIVE' WHERE email = 'deepak@tms.com';
UPDATE drivers SET status = 'ACTIVE' WHERE email = 'lakshmi@tms.com';
