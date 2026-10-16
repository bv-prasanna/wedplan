ALTER TABLE vendor_bookings ADD COLUMN IF NOT EXISTS service_date DATE;
ALTER TABLE vendor_bookings ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMPTZ;
CREATE TABLE IF NOT EXISTS vendor_unavailability(id UUID PRIMARY KEY,vendor_id UUID NOT NULL REFERENCES vendors(id),unavailable_date DATE NOT NULL,reason VARCHAR(200),UNIQUE(vendor_id,unavailable_date));
CREATE UNIQUE INDEX IF NOT EXISTS ux_active_vendor_day ON vendor_bookings(vendor_id,service_date) WHERE service_date IS NOT NULL AND status='CONFIRMED';
CREATE TABLE IF NOT EXISTS vendor_reviews(id UUID PRIMARY KEY,booking_id UUID NOT NULL UNIQUE REFERENCES vendor_bookings(id),rating INT NOT NULL CHECK(rating BETWEEN 1 AND 5),comment VARCHAR(1000),created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE IF NOT EXISTS user_notifications(id UUID PRIMARY KEY,user_id UUID NOT NULL REFERENCES app_users(id),kind VARCHAR(40) NOT NULL,title VARCHAR(200) NOT NULL,body VARCHAR(1000) NOT NULL,read_at TIMESTAMPTZ,created_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE INDEX IF NOT EXISTS idx_notifications_user ON user_notifications(user_id,created_at DESC);
CREATE TABLE IF NOT EXISTS payment_events(id UUID PRIMARY KEY,payment_id UUID NOT NULL REFERENCES payment_records(id),provider_event_id VARCHAR(200) NOT NULL UNIQUE,event_type VARCHAR(40) NOT NULL,received_at TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE IF NOT EXISTS audit_events(id UUID PRIMARY KEY,actor_id UUID REFERENCES app_users(id),action VARCHAR(100) NOT NULL,subject_id UUID,created_at TIMESTAMPTZ NOT NULL DEFAULT now());
