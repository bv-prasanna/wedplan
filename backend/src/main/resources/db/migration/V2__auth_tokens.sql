CREATE TABLE IF NOT EXISTS auth_tokens (id UUID PRIMARY KEY,user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,token_hash VARCHAR(64) NOT NULL UNIQUE,expires_at TIMESTAMPTZ NOT NULL);
CREATE INDEX IF NOT EXISTS idx_auth_tokens_expiry ON auth_tokens(expires_at);
