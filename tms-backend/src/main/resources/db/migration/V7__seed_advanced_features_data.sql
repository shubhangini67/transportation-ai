-- ============================================================================
-- V7__seed_advanced_features_data.sql
-- Backfills route_id on demo trips, and seeds data for tables added after V2:
--   • tenants
--   • expenses
--   • webhook_registrations
--   • invoices  +  invoice_items
--   • notifications
--
-- All IDs are deterministic UUIDs matching the naming pattern used in V2.
-- ============================================================================

DO $$
BEGIN
    -- ═══════════════════════════════════════════════════════════════════════
    -- BACKFILL route_id ON EXISTING TRIPS
    -- Routes are BIGSERIAL; V2 inserts them in order → IDs 1..12.
    --   1  Delhi → Jaipur           2  Mumbai → Pune
    --   3  Bengaluru → Chennai      4  Hyderabad → Vijayawada
    --   5  Ahmedabad → Surat        6  Jaipur → Udaipur
    --   7  Chennai → Madurai        8  Kolkata → Bhubaneswar
    --   9  Chennai → Puducherry    10  Delhi → Agra
    --  11  Delhi → Mumbai          12  Chennai → Bengaluru
    -- ═══════════════════════════════════════════════════════════════════════
    UPDATE trips SET route_id = 1  WHERE id = 'e0000000-0000-0000-0000-000000000001';  -- Delhi → Jaipur freight
    UPDATE trips SET route_id = 2  WHERE id = 'e0000000-0000-0000-0000-000000000002';  -- Mumbai → Pune
    UPDATE trips SET route_id = 6  WHERE id = 'e0000000-0000-0000-0000-000000000003';  -- Jaipur → Udaipur
    UPDATE trips SET route_id = 1  WHERE id = 'e0000000-0000-0000-0000-000000000004';  -- Delhi → Jaipur bus
    UPDATE trips SET route_id = 3  WHERE id = 'e0000000-0000-0000-0000-000000000005';  -- Bengaluru → Chennai
    UPDATE trips SET route_id = 8  WHERE id = 'e0000000-0000-0000-0000-000000000006';  -- Kolkata → Bhubaneswar
    UPDATE trips SET route_id = 5  WHERE id = 'e0000000-0000-0000-0000-000000000007';  -- Ahmedabad → Surat
    UPDATE trips SET route_id = 7  WHERE id = 'e0000000-0000-0000-0000-000000000008';  -- Chennai → Madurai
    UPDATE trips SET route_id = 2  WHERE id = 'e0000000-0000-0000-0000-000000000009';  -- Mumbai → Pune (completed)
    UPDATE trips SET route_id = 3  WHERE id = 'e0000000-0000-0000-0000-000000000010';  -- Bengaluru → Chennai (completed)
    UPDATE trips SET route_id = 9  WHERE id = 'e0000000-0000-0000-0000-000000000011';  -- Chennai → Puducherry (completed)
    UPDATE trips SET route_id = 6  WHERE id = 'e0000000-0000-0000-0000-000000000012';  -- Jaipur → Udaipur (completed)
    UPDATE trips SET route_id = 10 WHERE id = 'e0000000-0000-0000-0000-000000000013';  -- Delhi → Agra (completed)
    UPDATE trips SET route_id = 4  WHERE id = 'e0000000-0000-0000-0000-000000000014';  -- Hyderabad → Vijayawada (completed)


    -- ═══════════════════════════════════════════════════════════════════════
    -- TENANTS  (2 tenants — one active, one for demo)
    -- ═══════════════════════════════════════════════════════════════════════
    IF (SELECT count(*) FROM tenants) = 0 THEN
        INSERT INTO tenants (id, name, subdomain, active, created_at, updated_at, created_by)
        VALUES
          ('f0000000-0000-0000-0000-000000000001', 'Bharat Transport Co.',   'bharat',   true,  NOW() - INTERVAL '90 days', NOW(), 'admin'),
          ('f0000000-0000-0000-0000-000000000002', 'Delta Logistics Pvt Ltd','delta',    true,  NOW() - INTERVAL '60 days', NOW(), 'admin'),
          ('f0000000-0000-0000-0000-000000000003', 'Omega Freight Pvt Ltd',  'omega',    false, NOW() - INTERVAL '30 days', NOW(), 'admin');
    END IF;


    -- ═══════════════════════════════════════════════════════════════════════
    -- EXPENSES  (12 expenses across various trips, vehicles, and categories)
    -- ═══════════════════════════════════════════════════════════════════════
    IF (SELECT count(*) FROM expenses) = 0 THEN
        INSERT INTO expenses (id, trip_id, vehicle_id, category, amount, description, expense_date, created_at, updated_at, created_by)
        VALUES
          -- Completed trip 10 (Bengaluru→Chennai) expenses
          ('70000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000010', 'b0000000-0000-0000-0000-000000000001',
           'FUEL',              450.00, 'Diesel fill-up — departure Bengaluru',         NOW()::date - 8,  NOW() - INTERVAL '8 days', NOW(), 'dispatcher'),
          ('70000000-0000-0000-0000-000000000002', 'e0000000-0000-0000-0000-000000000010', 'b0000000-0000-0000-0000-000000000001',
           'TOLL',               85.00, 'NH48 FASTag charges',                          NOW()::date - 8,  NOW() - INTERVAL '8 days', NOW(), 'dispatcher'),
          ('70000000-0000-0000-0000-000000000003', 'e0000000-0000-0000-0000-000000000010', 'b0000000-0000-0000-0000-000000000001',
           'DRIVER_ALLOWANCE',  200.00, 'Driver overnight allowance — Ramesh Yadav',    NOW()::date - 7,  NOW() - INTERVAL '7 days', NOW(), 'dispatcher'),

          -- Completed trip 12 (Jaipur→Udaipur) expenses
          ('70000000-0000-0000-0000-000000000004', 'e0000000-0000-0000-0000-000000000012', 'b0000000-0000-0000-0000-000000000005',
           'FUEL',              380.00, 'Diesel — Jaipur depot fill',                   NOW()::date - 11, NOW() - INTERVAL '11 days', NOW(), 'dispatcher'),
          ('70000000-0000-0000-0000-000000000005', 'e0000000-0000-0000-0000-000000000012', 'b0000000-0000-0000-0000-000000000005',
           'TOLL',               45.00, 'NH48 / NH27 FASTag',                           NOW()::date - 10, NOW() - INTERVAL '10 days', NOW(), 'dispatcher'),

          -- Completed trip 14 (Hyderabad→Vijayawada) expenses
          ('70000000-0000-0000-0000-000000000006', 'e0000000-0000-0000-0000-000000000014', 'b0000000-0000-0000-0000-000000000003',
           'FUEL',              320.00, 'Diesel — Hyderabad warehouse fill',            NOW()::date - 9,  NOW() - INTERVAL '9 days', NOW(), 'dispatcher'),
          ('70000000-0000-0000-0000-000000000007', 'e0000000-0000-0000-0000-000000000014', 'b0000000-0000-0000-0000-000000000003',
           'MAINTENANCE',       150.00, 'Tire pressure check + wiper fluid top-up',     NOW()::date - 9,  NOW() - INTERVAL '9 days', NOW(), 'dispatcher'),

          -- In-progress trip 6 (Kolkata→Bhubaneswar) expenses
          ('70000000-0000-0000-0000-000000000008', 'e0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000006',
           'FUEL',              120.00, 'Fuel top-up before departure',                  NOW()::date,      NOW() - INTERVAL '4 hours', NOW(), 'dispatcher'),
          ('70000000-0000-0000-0000-000000000009', 'e0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000006',
           'TOLL',               55.00, 'NH16 FASTag',                                   NOW()::date,      NOW() - INTERVAL '2 hours', NOW(), 'dispatcher'),

          -- Vehicle maintenance (no trip)
          ('70000000-0000-0000-0000-000000000010', NULL, 'b0000000-0000-0000-0000-000000000002',
           'MAINTENANCE',      1250.00, 'Scheduled service — HR-26-CD-5678 engine oil + filters', NOW()::date - 5, NOW() - INTERVAL '5 days', NOW(), 'admin'),
          ('70000000-0000-0000-0000-000000000011', NULL, 'b0000000-0000-0000-0000-000000000008',
           'MAINTENANCE',       800.00, 'Brake pad replacement — HR-26-QR-5566',                   NOW()::date - 3, NOW() - INTERVAL '3 days', NOW(), 'admin'),

          -- Miscellaneous
          ('70000000-0000-0000-0000-000000000012', 'e0000000-0000-0000-0000-000000000009', 'b0000000-0000-0000-0000-000000000007',
           'OTHER',             65.00,  'Parking fee — Pune warehouse',                       NOW()::date - 5, NOW() - INTERVAL '5 days', NOW(), 'dispatcher');
    END IF;


    -- ═══════════════════════════════════════════════════════════════════════
    -- WEBHOOK REGISTRATIONS  (3 registrations)
    -- ═══════════════════════════════════════════════════════════════════════
    IF (SELECT count(*) FROM webhook_registrations) = 0 THEN
        INSERT INTO webhook_registrations (id, url, event_types, secret, active, description, created_at, updated_at, created_by)
        VALUES
          ('80000000-0000-0000-0000-000000000001',
           'https://hooks.example.com/tms/trips',
           'TRIP_CREATED,TRIP_STATUS_CHANGED,TRIP_COMPLETED',
           'whsec_abc123def456',
           true,
           'External dispatch system — trip events',
           NOW() - INTERVAL '30 days', NOW(), 'admin'),

          ('80000000-0000-0000-0000-000000000002',
           'https://hooks.example.com/tms/bookings',
           'BOOKING_CREATED,BOOKING_CANCELLED',
           'whsec_ghi789jkl012',
           true,
           'Customer notification service — booking events',
           NOW() - INTERVAL '25 days', NOW(), 'admin'),

          ('80000000-0000-0000-0000-000000000003',
           'https://old-system.example.com/webhooks',
           'LR_CREATED,LR_STATUS_CHANGED',
           'whsec_mno345pqr678',
           false,
           'Legacy LR integration — disabled',
           NOW() - INTERVAL '60 days', NOW(), 'admin');
    END IF;


    -- ═══════════════════════════════════════════════════════════════════════
    -- INVOICES + INVOICE ITEMS  (4 invoices tied to completed trips)
    -- ═══════════════════════════════════════════════════════════════════════
    IF (SELECT count(*) FROM invoices) = 0 THEN
        -- Invoice 1 — Trip 10 (Bengaluru → Chennai, structural steel)
        INSERT INTO invoices (id, invoice_number, trip_id, client_name, client_email,
                              subtotal, tax_rate, tax_amount, total_amount,
                              status, notes, issued_date, due_date,
                              created_at, updated_at, created_by)
        VALUES
          ('90000000-0000-0000-0000-000000000001', 'INV-2026-0001',
           'e0000000-0000-0000-0000-000000000010',
           'Tata Steel', 'billing@tatasteel.com',
           15000.00, 18.00, 2700.00, 17700.00,
           'PAID', 'Structural steel transport Bengaluru→Chennai — paid in full',
           NOW()::date - 6, NOW()::date + 24,
           NOW() - INTERVAL '6 days', NOW() - INTERVAL '2 days', 'dispatcher');

        INSERT INTO invoice_items (id, invoice_id, description, category, quantity, unit_price, amount, expense_id, created_at, updated_at, created_by)
        VALUES
          ('91000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000001',
           'Freight charge — Bengaluru to Chennai (346 km)', NULL, 1, 14265.00, 14265.00, NULL,
           NOW() - INTERVAL '6 days', NOW(), 'dispatcher'),
          ('91000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000001',
           'Fuel surcharge', 'FUEL', 1, 450.00, 450.00, '70000000-0000-0000-0000-000000000001',
           NOW() - INTERVAL '6 days', NOW(), 'dispatcher'),
          ('91000000-0000-0000-0000-000000000003', '90000000-0000-0000-0000-000000000001',
           'Toll charges', 'TOLL', 1, 85.00, 85.00, '70000000-0000-0000-0000-000000000002',
           NOW() - INTERVAL '6 days', NOW(), 'dispatcher'),
          ('91000000-0000-0000-0000-000000000004', '90000000-0000-0000-0000-000000000001',
           'Driver allowance', 'DRIVER_ALLOWANCE', 1, 200.00, 200.00, '70000000-0000-0000-0000-000000000003',
           NOW() - INTERVAL '6 days', NOW(), 'dispatcher');


        -- Invoice 2 — Trip 12 (Jaipur → Udaipur, paint & chemicals)
        INSERT INTO invoices (id, invoice_number, trip_id, client_name, client_email,
                              subtotal, tax_rate, tax_amount, total_amount,
                              status, notes, issued_date, due_date,
                              created_at, updated_at, created_by)
        VALUES
          ('90000000-0000-0000-0000-000000000002', 'INV-2026-0002',
           'e0000000-0000-0000-0000-000000000012',
           'Asian Paints', 'accounts@asianpaints.com',
           8500.00, 18.00, 1530.00, 10030.00,
           'SENT', 'Paint & chemicals Jaipur→Udaipur — invoice sent',
           NOW()::date - 9, NOW()::date + 21,
           NOW() - INTERVAL '9 days', NOW(), 'dispatcher');

        INSERT INTO invoice_items (id, invoice_id, description, category, quantity, unit_price, amount, expense_id, created_at, updated_at, created_by)
        VALUES
          ('91000000-0000-0000-0000-000000000005', '90000000-0000-0000-0000-000000000002',
           'Freight charge — Jaipur to Udaipur (395 km)', NULL, 1, 8075.00, 8075.00, NULL,
           NOW() - INTERVAL '9 days', NOW(), 'dispatcher'),
          ('91000000-0000-0000-0000-000000000006', '90000000-0000-0000-0000-000000000002',
           'Fuel surcharge', 'FUEL', 1, 380.00, 380.00, '70000000-0000-0000-0000-000000000004',
           NOW() - INTERVAL '9 days', NOW(), 'dispatcher'),
          ('91000000-0000-0000-0000-000000000007', '90000000-0000-0000-0000-000000000002',
           'Toll charges', 'TOLL', 1, 45.00, 45.00, '70000000-0000-0000-0000-000000000005',
           NOW() - INTERVAL '9 days', NOW(), 'dispatcher');


        -- Invoice 3 — Trip 11 (Chennai → Puducherry, laptops)
        INSERT INTO invoices (id, invoice_number, trip_id, client_name, client_email,
                              subtotal, tax_rate, tax_amount, total_amount,
                              status, notes, issued_date, due_date,
                              created_at, updated_at, created_by)
        VALUES
          ('90000000-0000-0000-0000-000000000003', 'INV-2026-0003',
           'e0000000-0000-0000-0000-000000000011',
           'Reliance Retail', 'finance@relianceretail.com',
           5200.00, 18.00, 936.00, 6136.00,
           'DRAFT', 'Laptops & monitors Chennai→Puducherry — draft',
           NULL, NULL,
           NOW() - INTERVAL '2 days', NOW(), 'dispatcher');

        INSERT INTO invoice_items (id, invoice_id, description, category, quantity, unit_price, amount, expense_id, created_at, updated_at, created_by)
        VALUES
          ('91000000-0000-0000-0000-000000000008', '90000000-0000-0000-0000-000000000003',
           'Freight charge — Chennai to Puducherry (160 km)', NULL, 1, 5200.00, 5200.00, NULL,
           NOW() - INTERVAL '2 days', NOW(), 'dispatcher');


        -- Invoice 4 — Trip 13 (Delhi → Agra, sewing machines)
        INSERT INTO invoices (id, invoice_number, trip_id, client_name, client_email,
                              subtotal, tax_rate, tax_amount, total_amount,
                              status, notes, issued_date, due_date,
                              created_at, updated_at, created_by)
        VALUES
          ('90000000-0000-0000-0000-000000000004', 'INV-2026-0004',
           'e0000000-0000-0000-0000-000000000013',
           'Usha International', 'billing@usha.com',
           4800.00, 18.00, 864.00, 5664.00,
           'CANCELLED', 'Sewing machines Delhi→Agra — cancelled by client',
           NOW()::date - 5, NOW()::date + 25,
           NOW() - INTERVAL '5 days', NOW() - INTERVAL '1 day', 'dispatcher');

        INSERT INTO invoice_items (id, invoice_id, description, category, quantity, unit_price, amount, expense_id, created_at, updated_at, created_by)
        VALUES
          ('91000000-0000-0000-0000-000000000009', '90000000-0000-0000-0000-000000000004',
           'Freight charge — Delhi to Agra (233 km)', NULL, 1, 4800.00, 4800.00, NULL,
           NOW() - INTERVAL '5 days', NOW(), 'dispatcher');
    END IF;


    -- ═══════════════════════════════════════════════════════════════════════
    -- NOTIFICATIONS  (sample notifications for various users)
    -- ═══════════════════════════════════════════════════════════════════════
    IF (SELECT count(*) FROM notifications) = 0 THEN
        INSERT INTO notifications (id, user_id, title, message, type, read, link, created_at, updated_at, created_by)
        VALUES
          -- Admin notifications
          ('a1000000-0000-0000-0000-000000000001',
           'a0000000-0000-0000-0000-000000000001',
           'New trip completed', 'Trip Bengaluru→Chennai (DL-01-AB-1234) completed successfully.',
           'TRIP_UPDATE', true, '/trips', NOW() - INTERVAL '7 days', NOW(), 'system'),

          ('a1000000-0000-0000-0000-000000000002',
           'a0000000-0000-0000-0000-000000000001',
           'Vehicle maintenance alert', 'HR-26-CD-5678 scheduled for maintenance.',
           'ALERT', true, '/vehicles', NOW() - INTERVAL '5 days', NOW(), 'system'),

          ('a1000000-0000-0000-0000-000000000003',
           'a0000000-0000-0000-0000-000000000001',
           'Invoice paid', 'Invoice INV-2026-0001 marked as paid by Tata Steel.',
           'INVOICE_UPDATE', false, '/invoices', NOW() - INTERVAL '2 days', NOW(), 'system'),

          -- Dispatcher notifications
          ('a1000000-0000-0000-0000-000000000004',
           'a0000000-0000-0000-0000-000000000003',
           'New booking received', 'Neha Gupta booked 2 seats on Delhi→Jaipur bus.',
           'BOOKING_UPDATE', true, '/bookings', NOW() - INTERVAL '12 hours', NOW(), 'system'),

          ('a1000000-0000-0000-0000-000000000005',
           'a0000000-0000-0000-0000-000000000003',
           'Booking cancelled', 'Rohan Joshi cancelled booking on Delhi→Jaipur bus.',
           'BOOKING_UPDATE', false, '/bookings', NOW() - INTERVAL '6 hours', NOW(), 'system'),

          ('a1000000-0000-0000-0000-000000000006',
           'a0000000-0000-0000-0000-000000000003',
           'Trip in progress', 'Trip Kolkata→Bhubaneswar (DL-04-LM-1122) has started.',
           'TRIP_UPDATE', true, '/trips', NOW() - INTERVAL '4 hours', NOW(), 'system'),

          -- Driver notifications
          ('a1000000-0000-0000-0000-000000000007',
           'a0000000-0000-0000-0000-000000000006',
           'Trip assigned', 'You have been assigned to Delhi→Jaipur freight trip tomorrow.',
           'TRIP_UPDATE', true, '/trips', NOW() - INTERVAL '1 day', NOW(), 'system'),

          ('a1000000-0000-0000-0000-000000000008',
           'a0000000-0000-0000-0000-000000000007',
           'Trip completed', 'Your trip Kolkata→Bhubaneswar has been marked complete.',
           'TRIP_UPDATE', false, '/trips', NOW() - INTERVAL '5 days', NOW(), 'system'),

          -- Client notifications
          ('a1000000-0000-0000-0000-000000000009',
           'a0000000-0000-0000-0000-000000000010',
           'Booking confirmed', 'Your booking for Delhi→Jaipur (2 seats) is confirmed.',
           'BOOKING_UPDATE', true, '/bookings', NOW() - INTERVAL '11 hours', NOW(), 'system'),

          ('a1000000-0000-0000-0000-000000000010',
           'a0000000-0000-0000-0000-000000000010',
           'System maintenance notice', 'TMS will undergo maintenance on Sunday 2AM–4AM IST.',
           'SYSTEM', false, NULL, NOW() - INTERVAL '2 days', NOW(), 'system');
    END IF;


    -- ═══════════════════════════════════════════════════════════════════════
    -- Done
    -- ═══════════════════════════════════════════════════════════════════════
    RAISE NOTICE 'V7 seed complete: route_id backfilled on 14 trips, tenants / expenses / webhooks / invoices / notifications seeded.';

END $$;


