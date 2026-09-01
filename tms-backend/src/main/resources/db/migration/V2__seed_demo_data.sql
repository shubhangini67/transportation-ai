-- ============================================================================
-- V2__seed_demo_data.sql
-- Production-grade demo data for Transport Management System
--
-- Covers all entities with realistic data across every status.
--   • 14 users   (ADMIN, DISPATCHER, DRIVER, CLIENT)
--   • 12 vehicles (TRUCK, VAN, BUS, MINI_BUS — AVAILABLE / BUSY / MAINTENANCE)
--   • 10 drivers  (ACTIVE / INACTIVE)
--   • 12 routes   (major Indian city pairs)
--   • 15 lorry receipts (CREATED / IN_TRANSIT / DELIVERED)
--   • 14 trips    (PLANNED / IN_PROGRESS / COMPLETED)
--   • 18 bookings (CONFIRMED / CANCELLED / COMPLETED)
--
-- Login credentials (all BCrypt-encoded):
--   admin     / admin123      (ADMIN)
--   dispatcher/ dispatch123   (DISPATCHER)
--   driver1   / driver123     (DRIVER)
--   client1   / client123     (CLIENT)
-- ============================================================================

-- ─── Guard: skip if data already exists ─────────────────────────────────────
DO $$
BEGIN
    IF (SELECT count(*) FROM users) > 0 THEN
        RAISE NOTICE 'Demo data already present — skipping V2 seed.';
        RETURN;
    END IF;

-- ═══════════════════════════════════════════════════════════════════════════
-- USERS
-- ═══════════════════════════════════════════════════════════════════════════
-- BCrypt hashes:  admin123    = $2a$10$pyDdfcKxAzocbEIb1/Bnzui7HDbrgBc2Dlowlb8vW5m1zE5YbggpK
--                 dispatch123 = $2a$10$DTrJIn7wlPFXArswaZAXJu4fjoxj64uFLcC5649bMqzVIrzcxKTFK
--                 driver123   = $2a$10$pE4evsOcgZVLigC.58rph.Na4C.gCXC/3XNk97GzVLwaYJDNSyMja
--                 client123   = $2a$10$aaaLMZ3tX3bh2pJCSvABQeRBfDzEAKCaaHmX9axYQ/1iKJKm8LR6G

INSERT INTO users (id, username, email, password, full_name, role, active, created_at, updated_at, created_by)
VALUES
  -- Admins
  ('a0000000-0000-0000-0000-000000000001', 'admin',       'admin@tms.com',       '$2a$10$pyDdfcKxAzocbEIb1/Bnzui7HDbrgBc2Dlowlb8vW5m1zE5YbggpK', 'Ravi Mehta',           'ADMIN',      true, NOW() - INTERVAL '90 days', NOW(), 'system'),
  ('a0000000-0000-0000-0000-000000000002', 'admin2',      'admin2@tms.com',      '$2a$10$pyDdfcKxAzocbEIb1/Bnzui7HDbrgBc2Dlowlb8vW5m1zE5YbggpK', 'Priya Sharma',         'ADMIN',      true, NOW() - INTERVAL '85 days', NOW(), 'system'),
  -- Dispatchers
  ('a0000000-0000-0000-0000-000000000003', 'dispatcher',  'dispatcher@tms.com',  '$2a$10$DTrJIn7wlPFXArswaZAXJu4fjoxj64uFLcC5649bMqzVIrzcxKTFK', 'Amit Singh',           'DISPATCHER', true, NOW() - INTERVAL '80 days', NOW(), 'admin'),
  ('a0000000-0000-0000-0000-000000000004', 'dispatcher2', 'dispatcher2@tms.com', '$2a$10$DTrJIn7wlPFXArswaZAXJu4fjoxj64uFLcC5649bMqzVIrzcxKTFK', 'Anita Verma',          'DISPATCHER', true, NOW() - INTERVAL '75 days', NOW(), 'admin'),
  ('a0000000-0000-0000-0000-000000000005', 'dispatcher3', 'dispatcher3@tms.com', '$2a$10$DTrJIn7wlPFXArswaZAXJu4fjoxj64uFLcC5649bMqzVIrzcxKTFK', 'Suresh Iyer',          'DISPATCHER', true, NOW() - INTERVAL '60 days', NOW(), 'admin'),
  -- Drivers
  ('a0000000-0000-0000-0000-000000000006', 'driver1',     'driver1@tms.com',     '$2a$10$pE4evsOcgZVLigC.58rph.Na4C.gCXC/3XNk97GzVLwaYJDNSyMja', 'Ramesh Yadav',         'DRIVER',     true, NOW() - INTERVAL '70 days', NOW(), 'admin'),
  ('a0000000-0000-0000-0000-000000000007', 'driver2',     'driver2@tms.com',     '$2a$10$pE4evsOcgZVLigC.58rph.Na4C.gCXC/3XNk97GzVLwaYJDNSyMja', 'Kavita Reddy',         'DRIVER',     true, NOW() - INTERVAL '68 days', NOW(), 'admin'),
  ('a0000000-0000-0000-0000-000000000008', 'driver3',     'driver3@tms.com',     '$2a$10$pE4evsOcgZVLigC.58rph.Na4C.gCXC/3XNk97GzVLwaYJDNSyMja', 'Raj Patel',            'DRIVER',     true, NOW() - INTERVAL '65 days', NOW(), 'admin'),
  ('a0000000-0000-0000-0000-000000000009', 'driver4',     'driver4@tms.com',     '$2a$10$pE4evsOcgZVLigC.58rph.Na4C.gCXC/3XNk97GzVLwaYJDNSyMja', 'Vikram Singh',         'DRIVER',     true, NOW() - INTERVAL '60 days', NOW(), 'admin'),
  -- Clients
  ('a0000000-0000-0000-0000-000000000010', 'client1',     'client1@tms.com',     '$2a$10$aaaLMZ3tX3bh2pJCSvABQeRBfDzEAKCaaHmX9axYQ/1iKJKm8LR6G', 'Neha Gupta',           'CLIENT',     true, NOW() - INTERVAL '55 days', NOW(), 'system'),
  ('a0000000-0000-0000-0000-000000000011', 'client2',     'client2@tms.com',     '$2a$10$aaaLMZ3tX3bh2pJCSvABQeRBfDzEAKCaaHmX9axYQ/1iKJKm8LR6G', 'Ahmed Khan',           'CLIENT',     true, NOW() - INTERVAL '50 days', NOW(), 'system'),
  ('a0000000-0000-0000-0000-000000000012', 'client3',     'client3@tms.com',     '$2a$10$aaaLMZ3tX3bh2pJCSvABQeRBfDzEAKCaaHmX9axYQ/1iKJKm8LR6G', 'Anjali Nair',          'CLIENT',     true, NOW() - INTERVAL '45 days', NOW(), 'system'),
  ('a0000000-0000-0000-0000-000000000013', 'client4',     'client4@tms.com',     '$2a$10$aaaLMZ3tX3bh2pJCSvABQeRBfDzEAKCaaHmX9axYQ/1iKJKm8LR6G', 'Rohan Joshi',          'CLIENT',     true, NOW() - INTERVAL '40 days', NOW(), 'system'),
  ('a0000000-0000-0000-0000-000000000014', 'client5',     'client5@tms.com',     '$2a$10$aaaLMZ3tX3bh2pJCSvABQeRBfDzEAKCaaHmX9axYQ/1iKJKm8LR6G', 'Pooja Desai',          'CLIENT',     true, NOW() - INTERVAL '30 days', NOW(), 'system');


-- ═══════════════════════════════════════════════════════════════════════════
-- VEHICLES  (5 trucks, 3 vans, 2 buses, 2 mini-buses)
-- ═══════════════════════════════════════════════════════════════════════════
INSERT INTO vehicles (id, vehicle_number, type, capacity, status, current_location, make, model, manufacture_year, created_at, updated_at, created_by)
VALUES
  -- Trucks
  ('b0000000-0000-0000-0000-000000000001', 'DL-01-AB-1234', 'TRUCK',    20, 'AVAILABLE',   'Main Depot, Delhi',            'Tata',          'Prima',         2023, NOW() - INTERVAL '60 days', NOW(), 'admin'),
  ('b0000000-0000-0000-0000-000000000002', 'HR-26-CD-5678', 'TRUCK',    30, 'MAINTENANCE',  'Service Center, Gurugram',     'Ashok Leyland', '1920',          2022, NOW() - INTERVAL '60 days', NOW(), 'admin'),
  ('b0000000-0000-0000-0000-000000000003', 'MH-12-EF-9012', 'TRUCK',    25, 'AVAILABLE',   'Warehouse, Mumbai',            'BharatBenz',    '3528',          2024, NOW() - INTERVAL '45 days', NOW(), 'admin'),
  ('b0000000-0000-0000-0000-000000000004', 'DL-03-GH-3456', 'TRUCK',    18, 'AVAILABLE',   'Main Depot, Delhi',            'Eicher',        'Pro 6048',      2023, NOW() - INTERVAL '50 days', NOW(), 'admin'),
  ('b0000000-0000-0000-0000-000000000005', 'KA-03-JK-7890', 'TRUCK',    35, 'AVAILABLE',   'Logistics Park, Bengaluru',    'Mahindra',      'Blazo X 42',    2024, NOW() - INTERVAL '30 days', NOW(), 'admin'),
  -- Vans
  ('b0000000-0000-0000-0000-000000000006', 'DL-04-LM-1122', 'VAN',       5, 'AVAILABLE',   'Main Depot, Delhi',            'Tata',          'Winger',        2024, NOW() - INTERVAL '55 days', NOW(), 'admin'),
  ('b0000000-0000-0000-0000-000000000007', 'MH-14-NP-3344', 'VAN',       4, 'AVAILABLE',   'Branch Office, Pune',          'Force',         'Traveller',     2023, NOW() - INTERVAL '50 days', NOW(), 'admin'),
  ('b0000000-0000-0000-0000-000000000008', 'HR-26-QR-5566', 'VAN',       6, 'MAINTENANCE',  'Service Center, Gurugram',     'Mahindra',      'Supro',         2022, NOW() - INTERVAL '50 days', NOW(), 'admin'),
  -- Buses
  ('b0000000-0000-0000-0000-000000000009', 'DL-1P-ST-7788', 'BUS',      50, 'AVAILABLE',   'ISBT Kashmere Gate, Delhi',    'Volvo',         '9400',          2024, NOW() - INTERVAL '40 days', NOW(), 'admin'),
  ('b0000000-0000-0000-0000-000000000010', 'TS-09-UV-9900', 'BUS',      45, 'AVAILABLE',   'MGBS Bus Station, Hyderabad',  'Ashok Leyland', 'Lynx',          2023, NOW() - INTERVAL '40 days', NOW(), 'admin'),
  -- Mini-buses
  ('b0000000-0000-0000-0000-000000000011', 'DL-05-WX-1212', 'MINI_BUS', 20, 'AVAILABLE',   'Main Depot, Delhi',            'Force',         'Traveller 26',  2024, NOW() - INTERVAL '35 days', NOW(), 'admin'),
  ('b0000000-0000-0000-0000-000000000012', 'TN-09-YZ-3434', 'MINI_BUS', 15, 'AVAILABLE',   'Branch Office, Chennai',       'Tata',          'Starbus Mini',  2023, NOW() - INTERVAL '35 days', NOW(), 'admin');


-- ═══════════════════════════════════════════════════════════════════════════
-- DRIVERS  (8 active, 2 inactive)
-- ═══════════════════════════════════════════════════════════════════════════
INSERT INTO drivers (id, name, phone, license_number, email, status, created_at, updated_at, created_by)
VALUES
  ('c0000000-0000-0000-0000-000000000001', 'Ramesh Yadav',     '+91-98765-43210', 'DL-0420240001234', 'ramesh@tms.com',  'ACTIVE',   NOW() - INTERVAL '70 days', NOW(), 'admin'),
  ('c0000000-0000-0000-0000-000000000002', 'Kavita Reddy',     '+91-98765-43211', 'MH-1220240002345', 'kavita@tms.com',  'ACTIVE',   NOW() - INTERVAL '68 days', NOW(), 'admin'),
  ('c0000000-0000-0000-0000-000000000003', 'Raj Patel',        '+91-98765-43212', 'GJ-0120240003456', 'raj@tms.com',     'ACTIVE',   NOW() - INTERVAL '65 days', NOW(), 'admin'),
  ('c0000000-0000-0000-0000-000000000004', 'Vikram Singh',     '+91-98765-43213', 'RJ-1420240004567', 'vikram@tms.com',  'ACTIVE',   NOW() - INTERVAL '60 days', NOW(), 'admin'),
  ('c0000000-0000-0000-0000-000000000005', 'Arjun Mehta',      '+91-98765-43214', 'KA-0320240005678', 'arjun@tms.com',   'ACTIVE',   NOW() - INTERVAL '55 days', NOW(), 'admin'),
  ('c0000000-0000-0000-0000-000000000006', 'Sunita Rao',       '+91-98765-43215', 'TN-0920240006789', 'sunita@tms.com',  'ACTIVE',   NOW() - INTERVAL '55 days', NOW(), 'admin'),
  ('c0000000-0000-0000-0000-000000000007', 'Manoj Tiwari',     '+91-98765-43216', 'UP-3220240007890', 'manoj@tms.com',   'ACTIVE',   NOW() - INTERVAL '50 days', NOW(), 'admin'),
  ('c0000000-0000-0000-0000-000000000008', 'Deepak Sharma',    '+91-98765-43217', 'HR-2620240008901', 'deepak@tms.com',  'INACTIVE', NOW() - INTERVAL '45 days', NOW(), 'admin'),
  ('c0000000-0000-0000-0000-000000000009', 'Lakshmi Pillai',   '+91-98765-43218', 'KL-0720240009012', 'lakshmi@tms.com', 'INACTIVE', NOW() - INTERVAL '45 days', NOW(), 'admin'),
  ('c0000000-0000-0000-0000-000000000010', 'Farhan Sheikh',    '+91-98765-43219', 'TS-0920240010123', 'farhan@tms.com',  'ACTIVE',   NOW() - INTERVAL '40 days', NOW(), 'admin');


-- ═══════════════════════════════════════════════════════════════════════════
-- ROUTES  (12 major Indian city pairs)
-- ═══════════════════════════════════════════════════════════════════════════
INSERT INTO routes (origin, destination, distance, estimated_time_minutes, description, active, created_at, updated_at, created_by)
VALUES
  ('Delhi',      'Jaipur',       268.0,  240, 'Delhi to Jaipur via NH48',                      true, NOW() - INTERVAL '60 days', NOW(), 'admin'),
  ('Mumbai',     'Pune',         148.0,  180, 'Mumbai to Pune via NH48',                       true, NOW() - INTERVAL '60 days', NOW(), 'admin'),
  ('Bengaluru',  'Chennai',      346.0,  360, 'Bengaluru to Chennai via NH48',                 true, NOW() - INTERVAL '60 days', NOW(), 'admin'),
  ('Hyderabad',  'Vijayawada',   275.0,  280, 'Hyderabad to Vijayawada via NH65',              true, NOW() - INTERVAL '55 days', NOW(), 'admin'),
  ('Ahmedabad',  'Surat',        263.0,  240, 'Ahmedabad to Surat via NH48',                   true, NOW() - INTERVAL '55 days', NOW(), 'admin'),
  ('Jaipur',     'Udaipur',      395.0,  360, 'Jaipur to Udaipur via NH48 / NH27',             true, NOW() - INTERVAL '50 days', NOW(), 'admin'),
  ('Chennai',    'Madurai',      462.0,  420, 'Chennai to Madurai via NH38',                   true, NOW() - INTERVAL '50 days', NOW(), 'admin'),
  ('Kolkata',    'Bhubaneswar',  442.0,  390, 'Kolkata to Bhubaneswar via NH16',               true, NOW() - INTERVAL '45 days', NOW(), 'admin'),
  ('Chennai',    'Puducherry',   160.0,  180, 'Chennai to Puducherry via ECR / NH32',          true, NOW() - INTERVAL '45 days', NOW(), 'admin'),
  ('Delhi',      'Agra',         233.0,  180, 'Delhi to Agra via Yamuna Expressway',           true, NOW() - INTERVAL '40 days', NOW(), 'admin'),
  ('Delhi',      'Mumbai',      1415.0, 1080, 'Delhi to Mumbai via NH48 (long-haul)',          true, NOW() - INTERVAL '40 days', NOW(), 'admin'),
  ('Chennai',    'Bengaluru',    346.0,  360, 'Chennai to Bengaluru via NH48 (return leg)',    true, NOW() - INTERVAL '35 days', NOW(), 'admin');


-- ═══════════════════════════════════════════════════════════════════════════
-- LORRY RECEIPTS  (5 CREATED, 4 IN_TRANSIT, 6 DELIVERED)
-- ═══════════════════════════════════════════════════════════════════════════
INSERT INTO lorry_receipts (id, lr_number, consignor, consignee, origin, destination, material, weight, quantity, status, created_at, updated_at, created_by)
VALUES
  -- CREATED (awaiting pickup)
  ('d0000000-0000-0000-0000-000000000001', 'LR-2026-0001', 'Tata Steel',          'JSW Steel',             'Delhi',      'Jaipur',       'Steel Pipes',                 5000.0,  100, 'CREATED',    NOW() - INTERVAL '5 days',  NOW(), 'dispatcher'),
  ('d0000000-0000-0000-0000-000000000002', 'LR-2026-0002', 'Reliance Retail',     'DMart',                 'Mumbai',     'Pune',         'Electronics',                 2000.0,   50, 'CREATED',    NOW() - INTERVAL '4 days',  NOW(), 'dispatcher'),
  ('d0000000-0000-0000-0000-000000000003', 'LR-2026-0003', 'Sun Pharma',          'Cipla',                 'Hyderabad',  'Vijayawada',   'Pharmaceutical Raw Materials', 3500.0,   75, 'CREATED',    NOW() - INTERVAL '3 days',  NOW(), 'dispatcher'),
  ('d0000000-0000-0000-0000-000000000004', 'LR-2026-0004', 'Amul',                'Mother Dairy',          'Jaipur',     'Udaipur',      'Frozen Foods',                8000.0,  200, 'CREATED',    NOW() - INTERVAL '2 days',  NOW(), 'dispatcher'),
  ('d0000000-0000-0000-0000-000000000005', 'LR-2026-0005', 'Arvind Mills',        'Raymond',               'Bengaluru',  'Chennai',      'Cotton Fabric Rolls',         4500.0,  120, 'CREATED',    NOW() - INTERVAL '1 day',   NOW(), 'dispatcher'),
  -- IN_TRANSIT
  ('d0000000-0000-0000-0000-000000000006', 'LR-2026-0006', 'Bharat Forge',        'Maruti Suzuki',         'Kolkata',    'Bhubaneswar',  'Auto Spare Parts',            1800.0,  300, 'IN_TRANSIT', NOW() - INTERVAL '2 days',  NOW(), 'dispatcher'),
  ('d0000000-0000-0000-0000-000000000007', 'LR-2026-0007', 'UltraTech Cement',    'L&T Construction',      'Delhi',      'Jaipur',       'Cement Bags',                12000.0,  240, 'IN_TRANSIT', NOW() - INTERVAL '3 days',  NOW(), 'dispatcher'),
  ('d0000000-0000-0000-0000-000000000008', 'LR-2026-0008', 'Infosys',             'TCS',                   'Ahmedabad',  'Surat',        'Server Equipment',             900.0,   15, 'IN_TRANSIT', NOW() - INTERVAL '1 day',   NOW(), 'dispatcher'),
  ('d0000000-0000-0000-0000-000000000009', 'LR-2026-0009', 'BigBasket',           'Reliance Fresh',        'Chennai',    'Madurai',      'Organic Vegetables',          6000.0,  150, 'IN_TRANSIT', NOW() - INTERVAL '1 day',   NOW(), 'dispatcher'),
  -- DELIVERED
  ('d0000000-0000-0000-0000-000000000010', 'LR-2026-0010', 'Godrej Interio',      'Pepperfry',             'Mumbai',     'Pune',         'Furniture',                   7500.0,   45, 'DELIVERED',  NOW() - INTERVAL '10 days', NOW(), 'dispatcher'),
  ('d0000000-0000-0000-0000-000000000011', 'LR-2026-0011', 'Tata Steel',          'L&T Construction',      'Bengaluru',  'Chennai',      'Structural Steel',           15000.0,   60, 'DELIVERED',  NOW() - INTERVAL '14 days', NOW(), 'dispatcher'),
  ('d0000000-0000-0000-0000-000000000012', 'LR-2026-0012', 'Reliance Retail',     'Croma',                 'Chennai',    'Puducherry',   'Laptops & Monitors',          1200.0,   80, 'DELIVERED',  NOW() - INTERVAL '8 days',  NOW(), 'dispatcher'),
  ('d0000000-0000-0000-0000-000000000013', 'LR-2026-0013', 'Asian Paints',        'Berger Paints',         'Jaipur',     'Udaipur',      'Paint & Chemicals',           4000.0,  180, 'DELIVERED',  NOW() - INTERVAL '18 days', NOW(), 'dispatcher'),
  ('d0000000-0000-0000-0000-000000000014', 'LR-2026-0014', 'Usha International',  'Singer India',          'Delhi',      'Agra',         'Sewing Machines',             2200.0,   30, 'DELIVERED',  NOW() - INTERVAL '12 days', NOW(), 'dispatcher'),
  ('d0000000-0000-0000-0000-000000000015', 'LR-2026-0015', 'Bharat Forge',        'Tata Motors',           'Hyderabad',  'Vijayawada',   'Engine Components',           3000.0,  500, 'DELIVERED',  NOW() - INTERVAL '16 days', NOW(), 'dispatcher');


-- ═══════════════════════════════════════════════════════════════════════════
-- TRIPS  (5 PLANNED, 3 IN_PROGRESS, 6 COMPLETED)
-- ═══════════════════════════════════════════════════════════════════════════
INSERT INTO trips (id, vehicle_id, driver_id, status, start_time, end_time, notes, created_at, updated_at, created_by)
VALUES
  -- ── PLANNED (future) ──────────────────────────────────────────────────
  ('e0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001',
    'PLANNED',
    NOW() + INTERVAL '1 day'  + TIME '08:00', NOW() + INTERVAL '1 day'  + TIME '16:00',
    'Delhi → Jaipur freight — steel pipes & cement',
    NOW() - INTERVAL '1 day', NOW(), 'dispatcher'),

  ('e0000000-0000-0000-0000-000000000002',
    'b0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000003',
    'PLANNED',
    NOW() + INTERVAL '2 days' + TIME '06:00', NOW() + INTERVAL '2 days' + TIME '12:00',
    'Mumbai → Pune electronics delivery',
    NOW() - INTERVAL '1 day', NOW(), 'dispatcher'),

  ('e0000000-0000-0000-0000-000000000003',
    'b0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000007',
    'PLANNED',
    NOW() + INTERVAL '3 days' + TIME '05:30', NOW() + INTERVAL '3 days' + TIME '11:30',
    'Jaipur → Udaipur frozen food shipment',
    NOW(), NOW(), 'dispatcher'),

  ('e0000000-0000-0000-0000-000000000004',
    'b0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000005',
    'PLANNED',
    NOW() + INTERVAL '1 day'  + TIME '07:00', NOW() + INTERVAL '1 day'  + TIME '11:00',
    'Delhi → Jaipur passenger bus service',
    NOW(), NOW(), 'dispatcher'),

  ('e0000000-0000-0000-0000-000000000005',
    'b0000000-0000-0000-0000-000000000011', 'c0000000-0000-0000-0000-000000000006',
    'PLANNED',
    NOW() + INTERVAL '4 days' + TIME '09:00', NOW() + INTERVAL '4 days' + TIME '14:00',
    'Bengaluru → Chennai shuttle service',
    NOW(), NOW(), 'dispatcher'),

  -- ── IN_PROGRESS (ongoing) ─────────────────────────────────────────────
  ('e0000000-0000-0000-0000-000000000006',
    'b0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000002',
    'IN_PROGRESS',
    NOW() - INTERVAL '3 hours', NOW() + INTERVAL '3 hours',
    'Kolkata → Bhubaneswar auto parts — in transit',
    NOW() - INTERVAL '4 hours', NOW(), 'dispatcher'),

  ('e0000000-0000-0000-0000-000000000007',
    'b0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000004',
    'IN_PROGRESS',
    NOW() - INTERVAL '2 hours', NOW() + INTERVAL '4 hours',
    'Ahmedabad → Surat server equipment',
    NOW() - INTERVAL '3 hours', NOW(), 'dispatcher'),

  ('e0000000-0000-0000-0000-000000000008',
    'b0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000010',
    'IN_PROGRESS',
    NOW() - INTERVAL '1 hour', NOW() + INTERVAL '5 hours',
    'Chennai → Madurai express — organic goods',
    NOW() - INTERVAL '2 hours', NOW(), 'dispatcher'),

  -- ── COMPLETED (past) ──────────────────────────────────────────────────
  ('e0000000-0000-0000-0000-000000000009',
    'b0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000002',
    'COMPLETED',
    NOW() - INTERVAL '5 days' + TIME '08:00', NOW() - INTERVAL '5 days' + TIME '14:00',
    'Mumbai → Pune furniture delivery — completed on time',
    NOW() - INTERVAL '6 days', NOW() - INTERVAL '5 days', 'dispatcher'),

  ('e0000000-0000-0000-0000-000000000010',
    'b0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001',
    'COMPLETED',
    NOW() - INTERVAL '7 days' + TIME '06:00', NOW() - INTERVAL '7 days' + TIME '16:00',
    'Bengaluru → Chennai structural steel — heavy load',
    NOW() - INTERVAL '8 days', NOW() - INTERVAL '7 days', 'dispatcher'),

  ('e0000000-0000-0000-0000-000000000011',
    'b0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000005',
    'COMPLETED',
    NOW() - INTERVAL '3 days' + TIME '09:00', NOW() - INTERVAL '3 days' + TIME '13:00',
    'Chennai → Puducherry laptops delivery',
    NOW() - INTERVAL '4 days', NOW() - INTERVAL '3 days', 'dispatcher'),

  ('e0000000-0000-0000-0000-000000000012',
    'b0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000007',
    'COMPLETED',
    NOW() - INTERVAL '10 days' + TIME '05:00', NOW() - INTERVAL '10 days' + TIME '11:00',
    'Jaipur → Udaipur paint & chemicals',
    NOW() - INTERVAL '11 days', NOW() - INTERVAL '10 days', 'dispatcher'),

  ('e0000000-0000-0000-0000-000000000013',
    'b0000000-0000-0000-0000-000000000012', 'c0000000-0000-0000-0000-000000000010',
    'COMPLETED',
    NOW() - INTERVAL '6 days' + TIME '10:00', NOW() - INTERVAL '6 days' + TIME '14:00',
    'Delhi → Agra sewing machines',
    NOW() - INTERVAL '7 days', NOW() - INTERVAL '6 days', 'dispatcher'),

  ('e0000000-0000-0000-0000-000000000014',
    'b0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000004',
    'COMPLETED',
    NOW() - INTERVAL '8 days' + TIME '04:00', NOW() - INTERVAL '8 days' + TIME '12:00',
    'Hyderabad → Vijayawada engine components — completed',
    NOW() - INTERVAL '9 days', NOW() - INTERVAL '8 days', 'dispatcher');


-- ═══════════════════════════════════════════════════════════════════════════
-- TRIP ↔ LORRY RECEIPT ASSOCIATIONS
-- ═══════════════════════════════════════════════════════════════════════════
INSERT INTO trip_lrs (trip_id, lr_id) VALUES
  -- Trip 1 (Delhi→Jaipur planned): LR-0001 + LR-0007
  ('e0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001'),
  ('e0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000007'),
  -- Trip 2 (Mumbai→Pune planned): LR-0002
  ('e0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002'),
  -- Trip 3 (Jaipur→Udaipur planned): LR-0004
  ('e0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000004'),
  -- Trip 5 (Bengaluru→Chennai planned): LR-0005
  ('e0000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000005'),
  -- Trip 6 (Kolkata→Bhubaneswar in-progress): LR-0006
  ('e0000000-0000-0000-0000-000000000006', 'd0000000-0000-0000-0000-000000000006'),
  -- Trip 7 (Ahmedabad→Surat in-progress): LR-0008
  ('e0000000-0000-0000-0000-000000000007', 'd0000000-0000-0000-0000-000000000008'),
  -- Trip 8 (Chennai→Madurai in-progress): LR-0009
  ('e0000000-0000-0000-0000-000000000008', 'd0000000-0000-0000-0000-000000000009'),
  -- Trip 9 (Mumbai→Pune completed): LR-0010
  ('e0000000-0000-0000-0000-000000000009', 'd0000000-0000-0000-0000-000000000010'),
  -- Trip 10 (Bengaluru→Chennai completed): LR-0011
  ('e0000000-0000-0000-0000-000000000010', 'd0000000-0000-0000-0000-000000000011'),
  -- Trip 11 (Chennai→Puducherry completed): LR-0012
  ('e0000000-0000-0000-0000-000000000011', 'd0000000-0000-0000-0000-000000000012'),
  -- Trip 12 (Jaipur→Udaipur completed): LR-0013
  ('e0000000-0000-0000-0000-000000000012', 'd0000000-0000-0000-0000-000000000013'),
  -- Trip 13 (Delhi→Agra completed): LR-0014
  ('e0000000-0000-0000-0000-000000000013', 'd0000000-0000-0000-0000-000000000014'),
  -- Trip 14 (Hyderabad→Vijayawada completed): LR-0015
  ('e0000000-0000-0000-0000-000000000014', 'd0000000-0000-0000-0000-000000000015');


-- ═══════════════════════════════════════════════════════════════════════════
-- UPDATE VEHICLE STATUS  (mark vehicles assigned to planned/in-progress trips as BUSY)
-- ═══════════════════════════════════════════════════════════════════════════
UPDATE vehicles SET status = 'BUSY', updated_at = NOW() WHERE id IN (
  'b0000000-0000-0000-0000-000000000001',  -- TRK-001 (trip 1 planned)
  'b0000000-0000-0000-0000-000000000003',  -- TRK-003 (trip 7 in-progress)
  'b0000000-0000-0000-0000-000000000004',  -- TRK-004 (trip 2 planned)
  'b0000000-0000-0000-0000-000000000005',  -- TRK-005 (trip 3 planned)
  'b0000000-0000-0000-0000-000000000006',  -- VAN-001 (trip 6 in-progress)
  'b0000000-0000-0000-0000-000000000009',  -- BUS-001 (trip 4 planned)
  'b0000000-0000-0000-0000-000000000010'   -- BUS-002 (trip 8 in-progress)
);


-- ═══════════════════════════════════════════════════════════════════════════
-- BOOKINGS  (12 CONFIRMED, 3 CANCELLED, 3 COMPLETED)
-- ═══════════════════════════════════════════════════════════════════════════
INSERT INTO bookings (customer_name, customer_phone, customer_email, trip_id, seat_count, status, notes, created_at, updated_at, created_by)
VALUES
  -- ── Trip 4: planned bus Delhi→Jaipur (cap 50) ──────────────────────────
  ('Neha Gupta',        '+91-98100-22001', 'neha@example.com',          'e0000000-0000-0000-0000-000000000004', 2, 'CONFIRMED', 'Window seats preferred',                    NOW() - INTERVAL '12 hours', NOW(), 'dispatcher'),
  ('Ahmed Khan',        '+91-98100-22002', 'ahmed@example.com',         'e0000000-0000-0000-0000-000000000004', 4, 'CONFIRMED', 'Family trip — 2 adults, 2 children',        NOW() - INTERVAL '10 hours', NOW(), 'dispatcher'),
  ('Anjali Nair',       '+91-98100-22003', 'anjali@example.com',        'e0000000-0000-0000-0000-000000000004', 1, 'CONFIRMED', 'Business travel',                           NOW() - INTERVAL '8 hours',  NOW(), 'dispatcher'),
  ('Rohan Joshi',       '+91-98100-22004', 'rohan@example.com',         'e0000000-0000-0000-0000-000000000004', 3, 'CANCELLED', 'Plans changed — cancelled by customer',      NOW() - INTERVAL '6 hours',  NOW(), 'dispatcher'),
  ('Pooja Desai',       '+91-98100-22005', 'pooja@example.com',         'e0000000-0000-0000-0000-000000000004', 2, 'CONFIRMED', NULL,                                        NOW() - INTERVAL '5 hours',  NOW(), 'dispatcher'),

  -- ── Trip 5: planned mini-bus Bengaluru→Chennai (cap 20) ───────────────
  ('Karan Malhotra',    '+91-98400-33001', 'karan.m@example.com',       'e0000000-0000-0000-0000-000000000005', 5, 'CONFIRMED', 'Group booking — colleagues',                NOW() - INTERVAL '2 days',   NOW(), 'dispatcher'),
  ('Priya Nair',        '+91-98400-33002', 'priya.n@example.com',       'e0000000-0000-0000-0000-000000000005', 2, 'CONFIRMED', NULL,                                        NOW() - INTERVAL '1 day',    NOW(), 'dispatcher'),
  ('Sanjay Kulkarni',   '+91-98400-33003', 'sanjay.k@example.com',      'e0000000-0000-0000-0000-000000000005', 1, 'CANCELLED', 'Rescheduled to next week',                   NOW() - INTERVAL '12 hours', NOW(), 'dispatcher'),

  -- ── Trip 1: planned freight Delhi→Jaipur ──────────────────────────────
  ('Tata Steel',        '+91-11-2345-0401', 'logistics@tatasteel.com',  'e0000000-0000-0000-0000-000000000001', 1, 'CONFIRMED', 'Freight booking — steel pipes consignment', NOW() - INTERVAL '1 day',    NOW(), 'dispatcher'),
  ('UltraTech Cement',  '+91-11-2345-0402', 'dispatch@ultratech.com',   'e0000000-0000-0000-0000-000000000001', 1, 'CONFIRMED', 'Freight booking — cement bags',             NOW() - INTERVAL '1 day',    NOW(), 'dispatcher'),

  -- ── Trip 9: completed Mumbai→Pune ─────────────────────────────────────
  ('Godrej Interio',    '+91-22-2345-0501', 'ops@godrej.com',           'e0000000-0000-0000-0000-000000000009', 1, 'COMPLETED', 'Furniture delivery completed successfully',  NOW() - INTERVAL '6 days',   NOW() - INTERVAL '5 days', 'dispatcher'),

  -- ── Trip 11: completed Chennai→Puducherry ─────────────────────────────
  ('Reliance Retail',   '+91-44-2345-0601', 'shipping@relianceretail.com','e0000000-0000-0000-0000-000000000011', 1, 'COMPLETED', 'Laptops delivered — POD signed',             NOW() - INTERVAL '4 days',   NOW() - INTERVAL '3 days', 'dispatcher'),
  ('Croma',             '+91-44-2345-0602', 'receiving@croma.com',      'e0000000-0000-0000-0000-000000000011', 1, 'COMPLETED', 'Monitors received in good condition',        NOW() - INTERVAL '4 days',   NOW() - INTERVAL '3 days', 'dispatcher'),

  -- ── Trip 6: in-progress Kolkata→Bhubaneswar ───────────────────────────
  ('Bharat Forge',      '+91-33-2345-0701', 'orders@bharatforge.com',   'e0000000-0000-0000-0000-000000000006', 1, 'CONFIRMED', 'Spare parts shipment — handle with care',    NOW() - INTERVAL '4 hours',  NOW(), 'dispatcher'),

  -- ── Trip 8: in-progress Chennai→Madurai ───────────────────────────────
  ('Meera Krishnan',    '+91-98401-08001', 'meera.k@example.com',       'e0000000-0000-0000-0000-000000000008', 3, 'CONFIRMED', 'Traveling with elderly parents',             NOW() - INTERVAL '3 hours',  NOW(), 'dispatcher'),
  ('Rahul Banerjee',    '+91-98401-08002', 'rahul.b@example.com',       'e0000000-0000-0000-0000-000000000008', 1, 'CONFIRMED', NULL,                                        NOW() - INTERVAL '2 hours',  NOW(), 'dispatcher'),
  ('Sneha Iyer',        '+91-98401-08003', 'sneha.i@example.com',       'e0000000-0000-0000-0000-000000000008', 2, 'CANCELLED', 'Train booked instead',                       NOW() - INTERVAL '1 hour',   NOW(), 'dispatcher');


-- ═══════════════════════════════════════════════════════════════════════════
-- Done
-- ═══════════════════════════════════════════════════════════════════════════
RAISE NOTICE 'Demo data seeded successfully: 14 users, 12 vehicles, 10 drivers, 12 routes, 15 LRs, 14 trips, 18 bookings.';

END $$;

