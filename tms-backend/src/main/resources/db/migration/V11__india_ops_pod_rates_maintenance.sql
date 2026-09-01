-- India GPS, maintenance windows, public tracking tokens, POD, freight rate cards.

ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS odometer_km INTEGER;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS last_service_date DATE;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS next_service_due_km INTEGER;

UPDATE vehicles SET latitude = 28.6139, longitude = 77.2090, odometer_km = 84200, last_service_date = CURRENT_DATE - 40, next_service_due_km = 95000 WHERE vehicle_number = 'DL-01-AB-1234';
UPDATE vehicles SET latitude = 28.4595, longitude = 77.0266, odometer_km = 186400, last_service_date = CURRENT_DATE - 210, next_service_due_km = 180000 WHERE vehicle_number = 'HR-26-CD-5678';
UPDATE vehicles SET latitude = 19.2183, longitude = 72.9781, odometer_km = 41200, last_service_date = CURRENT_DATE - 20, next_service_due_km = 60000 WHERE vehicle_number = 'MH-12-EF-9012';
UPDATE vehicles SET latitude = 28.7041, longitude = 77.1025, odometer_km = 67800, last_service_date = CURRENT_DATE - 55, next_service_due_km = 80000 WHERE vehicle_number = 'DL-03-GH-3456';
UPDATE vehicles SET latitude = 12.9716, longitude = 77.5946, odometer_km = 22100, last_service_date = CURRENT_DATE - 15, next_service_due_km = 40000 WHERE vehicle_number = 'KA-03-JK-7890';
UPDATE vehicles SET latitude = 22.5726, longitude = 88.3639, odometer_km = 33400, last_service_date = CURRENT_DATE - 30, next_service_due_km = 45000 WHERE vehicle_number = 'DL-04-LM-1122';
UPDATE vehicles SET latitude = 18.5204, longitude = 73.8567, odometer_km = 28900, last_service_date = CURRENT_DATE - 25, next_service_due_km = 40000 WHERE vehicle_number = 'MH-14-NP-3344';
UPDATE vehicles SET latitude = 28.4089, longitude = 77.3178, odometer_km = 97400, last_service_date = CURRENT_DATE - 190, next_service_due_km = 100000 WHERE vehicle_number = 'HR-26-QR-5566';
UPDATE vehicles SET latitude = 28.6676, longitude = 77.2273, odometer_km = 51200, last_service_date = CURRENT_DATE - 12, next_service_due_km = 70000 WHERE vehicle_number = 'DL-1P-ST-7788';
UPDATE vehicles SET latitude = 13.0827, longitude = 80.2707, odometer_km = 44800, last_service_date = CURRENT_DATE - 18, next_service_due_km = 60000 WHERE vehicle_number = 'TS-09-UV-9900';
UPDATE vehicles SET latitude = 28.5355, longitude = 77.3910, odometer_km = 19800, last_service_date = CURRENT_DATE - 8, next_service_due_km = 35000 WHERE vehicle_number = 'DL-05-WX-1212';
UPDATE vehicles SET latitude = 13.0475, longitude = 80.2480, odometer_km = 36100, last_service_date = CURRENT_DATE - 22, next_service_due_km = 50000 WHERE vehicle_number = 'TN-09-YZ-3434';

ALTER TABLE trips ADD COLUMN IF NOT EXISTS tracking_token VARCHAR(32);
UPDATE trips SET tracking_token = REPLACE(id::text, '-', '') WHERE tracking_token IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_trips_tracking_token ON trips(tracking_token);

CREATE TABLE IF NOT EXISTS proof_of_delivery (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL REFERENCES trips(id),
    lr_id UUID REFERENCES lorry_receipts(id),
    receiver_name VARCHAR(150) NOT NULL,
    receiver_phone VARCHAR(20),
    otp VARCHAR(6),
    notes VARCHAR(500),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    delivered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tenant_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);
CREATE INDEX IF NOT EXISTS idx_pod_trip ON proof_of_delivery(trip_id);

CREATE TABLE IF NOT EXISTS freight_rate_cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    origin VARCHAR(120) NOT NULL,
    destination VARCHAR(120) NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    rate_per_km NUMERIC(12,2) NOT NULL,
    min_charge NUMERIC(12,2) NOT NULL,
    gst_percent NUMERIC(5,2) NOT NULL DEFAULT 18.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

INSERT INTO freight_rate_cards (origin, destination, vehicle_type, rate_per_km, min_charge, gst_percent, active)
VALUES
    ('Delhi', 'Jaipur', 'TRUCK', 38.00, 8000.00, 18.00, true),
    ('Delhi', 'Jaipur', 'BUS', 22.00, 5000.00, 18.00, true),
    ('Mumbai', 'Pune', 'TRUCK', 42.00, 4500.00, 18.00, true),
    ('Mumbai', 'Pune', 'VAN', 28.00, 2500.00, 18.00, true),
    ('Bengaluru', 'Chennai', 'TRUCK', 36.00, 9000.00, 18.00, true),
    ('Hyderabad', 'Vijayawada', 'TRUCK', 35.00, 7500.00, 18.00, true),
    ('Ahmedabad', 'Surat', 'TRUCK', 34.00, 7000.00, 18.00, true),
    ('Jaipur', 'Udaipur', 'TRUCK', 33.00, 8500.00, 18.00, true),
    ('Chennai', 'Madurai', 'TRUCK', 32.00, 9000.00, 18.00, true),
    ('Kolkata', 'Bhubaneswar', 'VAN', 30.00, 6000.00, 18.00, true),
    ('Delhi', 'Mumbai', 'TRUCK', 31.00, 28000.00, 18.00, true),
    ('Chennai', 'Bengaluru', 'MINI_BUS', 26.00, 7000.00, 18.00, true);

INSERT INTO geofences (name, description, latitude, longitude, radius_meters, type, active)
SELECT * FROM (VALUES
    ('JNPT Nhava Sheva', 'JNPT container terminal — inbound ocean freight', 18.9490, 72.9490, 1200, 'DEPOT', true),
    ('Chennai Port CFS', 'Chennai port container freight station', 13.0827, 80.2910, 800, 'DELIVERY_ZONE', true),
    ('Hyderabad Gachibowli Hub', 'IT corridor delivery hub', 17.4401, 78.3489, 600, 'DELIVERY_ZONE', true),
    ('Jaipur Sitapura Depot', 'Sitapura industrial area staging yard', 26.7895, 75.8472, 700, 'DEPOT', true)
) AS extra(name, description, latitude, longitude, radius_meters, type, active)
WHERE NOT EXISTS (SELECT 1 FROM geofences g WHERE g.name = extra.name);

INSERT INTO proof_of_delivery (trip_id, lr_id, receiver_name, receiver_phone, otp, notes, latitude, longitude, delivered_at, created_by)
SELECT t.id, lr.id, 'Warehouse in-charge', '+91-98100-11111', '482913',
       'Consignment received in good condition', 18.5204, 73.8567, t.end_time, 'driver1'
FROM trips t
JOIN trip_lrs tl ON tl.trip_id = t.id
JOIN lorry_receipts lr ON lr.id = tl.lr_id
WHERE t.status = 'COMPLETED'
LIMIT 3;
