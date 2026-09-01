-- V4__add_tenant_and_avatar_support.sql
-- Adds tenant_id to all major entity tables for multi-tenant support
-- Adds avatar_url to users for profile avatars

-- Add tenant_id columns
ALTER TABLE users ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE drivers ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE routes ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE trips ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE lorry_receipts ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS tenant_id UUID;

-- Add avatar_url to users
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);

-- Create indexes for tenant_id
CREATE INDEX IF NOT EXISTS idx_users_tenant ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_vehicles_tenant ON vehicles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_drivers_tenant ON drivers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_routes_tenant ON routes(tenant_id);
CREATE INDEX IF NOT EXISTS idx_trips_tenant ON trips(tenant_id);
CREATE INDEX IF NOT EXISTS idx_bookings_tenant ON bookings(tenant_id);
CREATE INDEX IF NOT EXISTS idx_lrs_tenant ON lorry_receipts(tenant_id);
CREATE INDEX IF NOT EXISTS idx_expenses_tenant ON expenses(tenant_id);

