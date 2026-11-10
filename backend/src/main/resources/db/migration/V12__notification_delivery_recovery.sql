-- At-least-once outbox email delivery with recoverable leases and backoff.
-- A crash after provider acceptance may duplicate an email on retry; handlers
-- must keep wording safe and never use email success as payment confirmation.
ALTER TABLE notification_outbox ADD COLUMN IF NOT EXISTS claimed_at TIMESTAMPTZ;
ALTER TABLE notification_outbox ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE notification_outbox ADD COLUMN IF NOT EXISTS last_error_type VARCHAR(80);
CREATE INDEX IF NOT EXISTS idx_notification_outbox_ready
  ON notification_outbox(status,next_attempt_at,created_at);
