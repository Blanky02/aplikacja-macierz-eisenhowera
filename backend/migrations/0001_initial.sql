PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    email TEXT NOT NULL UNIQUE,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS saltedge_customers (
    user_id TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    customer_id TEXT NOT NULL UNIQUE,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS bank_connections (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider_code TEXT NOT NULL,
    provider_name TEXT NOT NULL,
    partner_consent_id TEXT,
    status TEXT NOT NULL DEFAULT 'connected',
    last_sync_at TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_bank_connections_user
    ON bank_connections(user_id, status);

CREATE TABLE IF NOT EXISTS bank_consents (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    customer_id TEXT NOT NULL,
    connection_id TEXT,
    provider_consent_id TEXT,
    scopes_json TEXT NOT NULL,
    from_date TEXT NOT NULL,
    period_days INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending',
    revoked_at TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_bank_consents_user
    ON bank_consents(user_id, status);

CREATE TABLE IF NOT EXISTS bank_accounts (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    connection_id TEXT NOT NULL REFERENCES bank_connections(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    nature TEXT,
    balance_minor INTEGER,
    currency_code TEXT NOT NULL,
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_bank_accounts_user
    ON bank_accounts(user_id, connection_id);

CREATE TABLE IF NOT EXISTS bank_transactions (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    connection_id TEXT NOT NULL REFERENCES bank_connections(id) ON DELETE CASCADE,
    account_id TEXT NOT NULL REFERENCES bank_accounts(id) ON DELETE CASCADE,
    amount_text TEXT NOT NULL,
    amount_minor INTEGER NOT NULL,
    currency_code TEXT NOT NULL,
    description TEXT NOT NULL,
    merchant_name TEXT,
    provider_category TEXT,
    made_on TEXT NOT NULL,
    status TEXT NOT NULL,
    quadrant_index INTEGER,
    quadrant_confidence REAL NOT NULL DEFAULT 0,
    quadrant_confirmed INTEGER NOT NULL DEFAULT 0,
    provider_updated_at TEXT,
    imported_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_bank_transactions_user_date
    ON bank_transactions(user_id, made_on DESC);

CREATE INDEX IF NOT EXISTS idx_bank_transactions_account
    ON bank_transactions(account_id, made_on DESC);

CREATE TABLE IF NOT EXISTS sync_runs (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    started_at TEXT NOT NULL,
    finished_at TEXT,
    status TEXT NOT NULL,
    accounts_seen INTEGER NOT NULL DEFAULT 0,
    transactions_seen INTEGER NOT NULL DEFAULT 0,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_sync_runs_user
    ON sync_runs(user_id, started_at DESC);
