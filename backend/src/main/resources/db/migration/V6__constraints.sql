-- Defensive indexes for active records and account access.
CREATE INDEX IF NOT EXISTS idx_vendor_bookings_vendor_date ON vendor_bookings(vendor_id,service_date);
CREATE INDEX IF NOT EXISTS idx_payment_records_booking ON payment_records(booking_id,status);
CREATE INDEX IF NOT EXISTS idx_enquiries_wedding ON enquiries(wedding_id);
CREATE INDEX IF NOT EXISTS idx_quotes_vendor ON vendor_quotes(vendor_id,status);
CREATE INDEX IF NOT EXISTS idx_auth_tokens_user ON auth_tokens(user_id);
