ALTER TABLE trips ADD COLUMN route_id BIGINT;
ALTER TABLE trips ADD CONSTRAINT fk_trips_route FOREIGN KEY (route_id) REFERENCES routes(id);
CREATE INDEX IF NOT EXISTS idx_trip_route ON trips(route_id);

