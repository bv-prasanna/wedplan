-- Financially sensitive requests must be authorized and queued before contacting
-- Razorpay. Older PENDING refund records remain intact and are never replayed.
ALTER TABLE payment_refunds ADD COLUMN IF NOT EXISTS requested_by UUID REFERENCES app_users(id);
ALTER TABLE payment_refunds ADD COLUMN IF NOT EXISTS request_key VARCHAR(80);
ALTER TABLE payment_refunds ADD COLUMN IF NOT EXISTS reason VARCHAR(500);
ALTER TABLE payment_refunds ADD COLUMN IF NOT EXISTS deduction_paise BIGINT NOT NULL DEFAULT 0;
ALTER TABLE payment_refunds ADD COLUMN IF NOT EXISTS dispatched_at TIMESTAMPTZ;
ALTER TABLE payment_refunds ADD COLUMN IF NOT EXISTS reconciled_at TIMESTAMPTZ;
ALTER TABLE payment_refunds ADD CONSTRAINT ck_refund_deduction_nonnegative CHECK (deduction_paise>=0);
CREATE UNIQUE INDEX IF NOT EXISTS ux_refund_payment_request_key ON payment_refunds(payment_id,request_key) WHERE request_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_refund_status_created ON payment_refunds(status,created_at);
