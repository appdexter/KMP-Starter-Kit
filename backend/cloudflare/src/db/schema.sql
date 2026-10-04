-- D1 Database Schema for Koko Cloudflare Backend

-- Table: users
-- Stores user mapping and attribution tracking parameters synced from KMP clients
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    app_user_id TEXT NOT NULL UNIQUE,
    fbclid TEXT,
    gclid TEXT,
    fbp TEXT,
    fbc TEXT,
    ip_address TEXT,
    user_agent TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_users_app_user_id ON users(app_user_id);
CREATE INDEX IF NOT EXISTS idx_users_fbclid ON users(fbclid) WHERE fbclid IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_gclid ON users(gclid) WHERE gclid IS NOT NULL;

-- Table: webhooks_log
-- Audit log of incoming webhook events from RevenueCat and Adapty
CREATE TABLE IF NOT EXISTS webhooks_log (
    id TEXT PRIMARY KEY,
    provider TEXT NOT NULL, -- 'revenuecat' | 'adapty'
    event_type TEXT NOT NULL,
    payload TEXT NOT NULL, -- JSON string
    status TEXT NOT NULL, -- 'RECEIVED' | 'QUEUED' | 'PROCESSED' | 'FAILED' | 'IGNORED'
    error TEXT,
    created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_webhooks_log_created_at ON webhooks_log(created_at);
CREATE INDEX IF NOT EXISTS idx_webhooks_log_provider ON webhooks_log(provider, event_type);
CREATE INDEX IF NOT EXISTS idx_webhooks_log_status ON webhooks_log(status);

-- Table: conversions
-- Tracks offline and server-side conversions sent to Meta CAPI and Google Ads
CREATE TABLE IF NOT EXISTS conversions (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL, -- references users.app_user_id
    event_name TEXT NOT NULL, -- e.g., 'Purchase', 'Subscribe', 'Refund'
    value REAL,
    currency TEXT,
    event_id TEXT NOT NULL UNIQUE, -- Unique deduplication key (e.g. meta_{txId}_{eventName})
    status TEXT NOT NULL, -- 'SENT' | 'FAILED' | 'SKIPPED_DEDUPE'
    error TEXT,
    created_at INTEGER NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_conversions_event_id ON conversions(event_id);
CREATE INDEX IF NOT EXISTS idx_conversions_user_id ON conversions(user_id);
CREATE INDEX IF NOT EXISTS idx_conversions_status ON conversions(status);
