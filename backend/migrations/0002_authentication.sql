CREATE TABLE IF NOT EXISTS auth_email_tokens (
    id TEXT PRIMARY KEY,
    email TEXT NOT NULL,
    token_hash TEXT NOT NULL UNIQUE,
    expires_at TEXT NOT NULL,
    used_at TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_auth_email_tokens_lookup
    ON auth_email_tokens(token_hash, expires_at, used_at);

CREATE TABLE IF NOT EXISTS auth_exchange_codes (
    code_hash TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TEXT NOT NULL,
    used_at TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_auth_exchange_codes_lookup
    ON auth_exchange_codes(code_hash, expires_at, used_at);
