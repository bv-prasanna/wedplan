-- Keep HTTP audit context and geolocation evidence attached to mutation records.
-- Client coordinates are self-reported; they must not be treated as verified presence.
ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS http_status INTEGER;
ALTER TABLE audit_events ADD COLUMN IF NOT EXISTS request_id VARCHAR(80);
ALTER TABLE audit_events ADD CONSTRAINT ck_audit_http_status
  CHECK (http_status IS NULL OR http_status BETWEEN 100 AND 599);
CREATE INDEX IF NOT EXISTS idx_audit_events_request_id ON audit_events(request_id);
