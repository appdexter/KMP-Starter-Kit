# Koko Cloudflare Edge Backend

Edge backend service for the **Koko KMP Contest Starter Kit** built on **Cloudflare Workers**, **Hono**, **Cloudflare D1**, and **Cloudflare Queues**.

This backend bridges client-side ad attribution (Meta `fbclid`/`fbp`/`fbc` and Google `gclid`) with server-side subscription lifecycle webhooks (from **RevenueCat** or **Adapty**) to deliver accurate server-side conversions to **Meta Conversions API (CAPI)** and **Google Ads / GA4 Measurement Protocol**.

---

## Architecture Flow

```
┌──────────────────────────────────────────────┐
│ KMP Mobile Client (Android / iOS / Web / JVM)│
└──────────────────────┬───────────────────────┘
                       │ POST /api/v1/attribution/sync
                       │ (app_user_id, fbclid, gclid, fbp, fbc, ip, ua)
                       ▼
            ┌─────────────────────┐
            │   Cloudflare D1     │
            │   (table: users)    │
            └──────────▲──────────┘
                       │
┌──────────────────────┴───────────────────────┐
│ RevenueCat / Adapty Subscription Webhooks    │
└──────────────────────┬───────────────────────┘
                       │ POST /api/v1/webhooks/{revenuecat, adapty}
                       ▼
            ┌─────────────────────┐
            │ D1: webhooks_log    │
            └──────────┬──────────┘
                       │ Enqueue Job
                       ▼
            ┌─────────────────────┐
            │ Cloudflare Queue    │
            │  (koko-subs-queue)  │
            └──────────┬──────────┘
                       │ Batch Consumer (Deduplication + Classification)
                       ▼
         ┌─────────────┴─────────────┐
         ▼                           ▼
┌──────────────────┐       ┌────────────────────┐
│ Meta CAPI        │       │ Google Ads API /   │
│ (Purchase/Refund)│       │ GA4 Measurement    │
└──────────────────┘       └────────────────────┘
```

---

## Features

1. **Lightweight & High-Performance:** Powered by [Hono](https://hono.dev/) on Cloudflare Workers edge runtime (global sub-50ms latency).
2. **Server-Side Attribution Sync:** Client syncs `fbclid`, `gclid`, `fbp`, `fbc`, `ip_address`, and `user_agent` mapped to `app_user_id`.
3. **Queue-Backed Async Webhook Processing:** Webhooks from RevenueCat/Adapty are instantly validated and acknowledged (HTTP 200), logging audit trails to D1 and delegating conversion dispatching to Cloudflare Queues with retry and dead-letter queue support.
4. **Intelligent Event Deduplication & Filtering:**
   - Skips duplicate events if previously marked `SENT` in D1 `conversions` table.
   - Filters out client-side handled `INITIAL_PURCHASE` to prevent double-counting.
   - Accurately reports server-side `RENEWAL`, `PRODUCT_CHANGE`, and `REFUND` events.
5. **Meta Conversions API (CAPI v20.0):**
   - Web Crypto SHA-256 normalized user identifiers (`external_id`, `em`, `ph`).
   - Supports `fbc`, `fbp`, `client_ip_address`, and `client_user_agent` for top-tier Match Quality scores.
   - Supports `META_TEST_EVENT_CODE` for real-time verification in Meta Events Manager.
6. **Google Ads & GA4 Conversions:**
   - Directly uploads click conversions via Google Ads API v18 (`uploadClickConversions`) when `gclid` and OAuth credentials are present.
   - Seamlessly dispatches conversions via Google Analytics 4 Measurement Protocol (`/mp/collect`) for universal app and web tracking.

---

## Directory Structure

```
backend/cloudflare/
├── package.json          # Dependencies & scripts
├── tsconfig.json         # ES2022 / NodeNext TypeScript configuration
├── wrangler.jsonc        # Cloudflare Worker, D1 & Queue configuration
├── .dev.vars.example     # Environment variable template
├── src/
│   ├── index.ts          # Worker fetch & queue exports, routing & health check
│   ├── db/
│   │   └── schema.sql    # D1 SQLite schema (users, webhooks_log, conversions)
│   ├── types/
│   │   └── index.ts      # TypeScript interfaces and Worker bindings
│   ├── services/
│   │   ├── crypto.ts     # SHA-256 and FBC normalization using Web Crypto API
│   │   ├── metaCapi.ts   # Meta Conversions API (v20.0) client
│   │   └── googleAds.ts  # Google Ads API & GA4 Measurement Protocol client
│   ├── queue/
│   │   └── consumer.ts   # Queue batch worker, deduplication & event classifier
│   └── routes/
│       ├── attribution.ts # /api/v1/attribution/sync
│       └── webhooks.ts    # /api/v1/webhooks/{revenuecat, adapty}
```

---

## Setup & Deployment

### 1. Install Dependencies

```bash
cd backend/cloudflare
npm install
```

### 2. Configure Cloudflare Resources

#### Create D1 Database:
```bash
npx wrangler d1 create koko-db
```
*Copy the returned `database_id` into `wrangler.jsonc` under `d1_databases[0].database_id`.*

#### Create Cloudflare Queue & Dead-Letter Queue:
```bash
npx wrangler queues create koko-subs-queue
npx wrangler queues create koko-subs-dlq
```

#### Run Database Migrations:
```bash
# For local development:
npm run db:migrate:local

# For remote production:
npm run db:migrate:remote
```

### 3. Configure Secrets

Set production secrets via Wrangler CLI:

```bash
# Meta CAPI
npx wrangler secret put META_PIXEL_ID
npx wrangler secret put META_CAPI_ACCESS_TOKEN
npx wrangler secret put META_TEST_EVENT_CODE # Optional for testing

# Google Tracking
npx wrangler secret put GOOGLE_ANALYTICS_MEASUREMENT_ID
npx wrangler secret put GOOGLE_ANALYTICS_API_SECRET
# Or Google Ads API (optional):
npx wrangler secret put GOOGLE_ADS_CUSTOMER_ID
npx wrangler secret put GOOGLE_ADS_CONVERSION_ACTION_ID
npx wrangler secret put GOOGLE_ADS_DEVELOPER_TOKEN
npx wrangler secret put GOOGLE_ADS_CLIENT_ID
npx wrangler secret put GOOGLE_ADS_CLIENT_SECRET
npx wrangler secret put GOOGLE_ADS_REFRESH_TOKEN

# Webhook Secrets
npx wrangler secret put WEBHOOK_SECRET_REVENUECAT
npx wrangler secret put WEBHOOK_SECRET_ADAPTY
```

### 4. Local Development

Copy `.dev.vars.example` to `.dev.vars` and run:

```bash
npm run dev
```

### 5. Deploy to Production

```bash
npm run deploy
```

---

## API Reference

### 1. Health Check
- **GET** `/health`
- **Response:**
  ```json
  {
    "status": "ok",
    "service": "koko-backend",
    "timestamp": 1700000000000,
    "environment": "production"
  }
  ```

### 2. Sync Attribution Identifiers
- **POST** `/api/v1/attribution/sync`
- **Request Body:**
  ```json
  {
    "app_user_id": "usr_94829a28",
    "fbclid": "IwAR3...",
    "gclid": "Cj0K...",
    "fbp": "fb.1.1700000000000.123456789",
    "fbc": "fb.1.1700000000000.IwAR3...",
    "ip_address": "203.0.113.195",
    "user_agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)..."
  }
  ```
- **Response:**
  ```json
  {
    "ok": true,
    "message": "Attribution identifiers synced successfully",
    "app_user_id": "usr_94829a28"
  }
  ```

### 3. RevenueCat Webhook
- **POST** `/api/v1/webhooks/revenuecat`
- **Header:** `Authorization: Bearer <WEBHOOK_SECRET_REVENUECAT>`
- **Response:**
  ```json
  {
    "ok": true,
    "logId": "2c51080a-5b1e-450a-8bf8-0902f2324900",
    "status": "queued"
  }
  ```

### 4. Adapty Webhook
- **POST** `/api/v1/webhooks/adapty`
- **Header:** `Authorization: Bearer <WEBHOOK_SECRET_ADAPTY>`
- **Response:**
  ```json
  {
    "ok": true,
    "logId": "8bfa2e41-0ca2-4886-9a2c-f67f2b1897c1",
    "status": "queued"
  }
  ```
