-- Keep account throttling state independent from account existence to avoid user enumeration.
CREATE TABLE IF NOT EXISTS auth_login_attempts (
 email_hash CHAR(64) PRIMARY KEY,
 failed_attempts INT NOT NULL DEFAULT 0 CHECK (failed_attempts >= 0),
 window_started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 blocked_until TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_auth_login_attempts_blocked_until ON auth_login_attempts(blocked_until);
