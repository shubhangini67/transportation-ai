-- ============================================================================
-- V6__schema_fixes_and_indexes.sql
-- Fixes column type mismatch, adds missing FK / query indexes, and adds
-- tenant_id indexes for tables created in V5.
-- ============================================================================

-- ─── 1. Fix refresh_tokens.expiry_date column type ─────────────────────────
-- Entity RefreshToken.expiryDate is java.time.Instant, which Hibernate 6 maps
-- to TIMESTAMP WITH TIME ZONE.  V3.1 created the column as plain TIMESTAMP.
-- This ALTER is safe — PostgreSQL casts TIMESTAMP → TIMESTAMPTZ implicitly.
ALTER TABLE refresh_tokens ALTER COLUMN expiry_date TYPE TIMESTAMPTZ;

-- ─── 2. Missing FK-column & query indexes ──────────────────────────────────
-- bookings
CREATE INDEX IF NOT EXISTS idx_bookings_trip      ON bookings(trip_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status    ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_bookings_created   ON bookings(created_at);

-- expenses
CREATE INDEX IF NOT EXISTS idx_expenses_trip      ON expenses(trip_id);
CREATE INDEX IF NOT EXISTS idx_expenses_vehicle   ON expenses(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_expenses_date      ON expenses(expense_date);

-- trips (FK columns & date-range queries)
CREATE INDEX IF NOT EXISTS idx_trips_driver       ON trips(driver_id);
CREATE INDEX IF NOT EXISTS idx_trips_vehicle      ON trips(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_trips_created      ON trips(created_at);

-- drivers (status queries)
CREATE INDEX IF NOT EXISTS idx_drivers_status     ON drivers(status);

-- refresh_tokens (user_id FK — used by revokeAllByUserId)
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);

-- invoices (issued_date range filter)
CREATE INDEX IF NOT EXISTS idx_invoices_issued    ON invoices(issued_date);

-- ─── 3. Missing tenant_id indexes for V5 tables ───────────────────────────
-- V4 added idx_*_tenant for the first 8 tables; V5 tables were missed.
CREATE INDEX IF NOT EXISTS idx_vlh_tenant            ON vehicle_location_history(tenant_id);
CREATE INDEX IF NOT EXISTS idx_invoices_tenant        ON invoices(tenant_id);
CREATE INDEX IF NOT EXISTS idx_notifications_tenant   ON notifications(tenant_id);
CREATE INDEX IF NOT EXISTS idx_attachments_tenant     ON file_attachments(tenant_id);


