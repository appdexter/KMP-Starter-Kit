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
                       │
            ┌──────────┴──────────┐
            ▼                     ▼
   [Free Tier Mode]       [Paid Tier Mode]
   c.executionCtx.waitUntil   Cloudflare Queue
   (Direct In-Flight Async)   (koko-subs-queue)
            │                     │
            └──────────┬──────────┘
                       │ Deduplication + Classification
                       ▼
          ┌────────────┴────────────┐
          ▼                         ▼
 ┌──────────────────┐     ┌────────────────────┐
 │ Meta CAPI        │     │ Google Ads API /   │
 │ (Purchase/Refund)│     │ GA4 Measurement    │
 └──────────────────┘     └────────────────────┘
```

---

## Hybrid Execution Engine: Free Tier ($0) vs Paid Tier

Koko Edge Backend features a **zero-configuration Hybrid Dispatcher** that automatically detects whether Cloudflare Queues are provisioned.

| Feature | Free Tier ($0/mo) | Paid Tier ($5/mo Workers Paid) |
|---|---|---|
| **Queue Binding** | `c.env.SUBS_QUEUE` is undefined | `c.env.SUBS_QUEUE` defined in `wrangler.jsonc` |
| **Execution Path** | `c.executionCtx.waitUntil(...)` (In-flight async) | `c.env.SUBS_QUEUE.send(...)` (Queue broker) |
| **Webhook Response** | Immediate HTTP 200 (`status: "dispatched_direct"`) | Immediate HTTP 200 (`status: "queued"`) |
| **Worker Invocations** | 1 invocation (HTTP handling + async conversion) | 2 invocations (HTTP producer + Queue consumer) |
| **Concurrency & Retry** | Native Fetch event lifecycle, logged to D1 | Configurable batch size, retries, and Dead Letter Queue |
| **Cost** | **$0 / month** (100,000 req/day free) | **$5 / month** base plan |

### How Hybrid Dispatcher Works

1. When a webhook arrives at `/api/v1/webhooks/revenuecat` or `/api/v1/webhooks/adapty`:
   - The payload is audited and saved into D1 (`webhooks_log`).
   - If `c.env.SUBS_QUEUE` exists: The message is sent to the queue. Status in DB is updated to `QUEUED`.
   - If `c.env.SUBS_QUEUE` does **NOT** exist (Free tier): The message is passed directly to `c.executionCtx.waitUntil(processSingleSubscriptionMessage(queueMessage, env))`. The HTTP 200 response is returned immediately to RevenueCat/Adapty, preventing timeout retries, while Cloudflare Workers keeps the runtime alive until the background conversions complete.
2. In both modes, `processSingleSubscriptionMessage`:
   - Checks event classification (`RENEWAL`, `INITIAL_PURCHASE`, `REFUND`, etc.).
   - Saves audit records in `conversions` with `SKIPPED_CLIENT_HANDLED` for initial purchases (preventing double counting with client SDK).
   - Enforces deduplication using unique `event_id` in D1 `conversions`.
   - Dispatches in parallel to Meta CAPI and Google Ads / GA4 MP via `Promise.allSettled`.
   - Records final status (`SENT` or `FAILED`) into D1.

---

## Features

1. **Lightweight & High-Performance:** Powered by [Hono](https://hono.dev/) on Cloudflare Workers edge runtime (global sub-50ms latency).
2. **Server-Side Attribution Sync:** Client syncs `fbclid`, `gclid`, `fbp`, `fbc`, `ip_address`, and `user_agent` mapped to `app_user_id`.
3. **Flexible Webhook Authentication:**
   - When secrets are configured, validates tokens from `Authorization: Bearer <secret>` or provider-specific headers.
   - In non-production environments (`ENVIRONMENT != 'production'`), requests are permitted with a warning if secrets are not yet set, enabling painless local testing.
   - In production, missing secrets reject requests with HTTP 401.
4. **Intelligent Event Deduplication & Filtering:**
   - Skips duplicate events if previously marked `SENT` in D1 `conversions` table.
   - Filters out client-side handled `INITIAL_PURCHASE` / `SUBSCRIPTION_STARTED` to prevent double-counting, logging them as `SKIPPED_CLIENT_HANDLED`.
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
├── tests/
│   └── mock-webhooks.http # Mock HTTP requests for VSCode REST Client & curl
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

## Setup & Deployment Guide

### 1. Install Dependencies

```bash
cd backend/cloudflare
npm install
```

### 2. Configure Cloudflare D1 Database

#### Create D1 Database:
```bash
npx wrangler d1 create koko-db
```
The command outputs your new database metadata:
```text
✅ Successfully created DB 'koko-db'!
add the following to your wrangler.jsonc:
"d1_databases": [
  {
    "binding": "DB",
    "database_name": "koko-db",
    "database_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
  }
]
```
Copy the returned `database_id` into `wrangler.jsonc` under `d1_databases[0].database_id`.

#### Run Database Migrations:
```bash
# For local SQLite simulation (.wrangler/state):
npm run db:migrate:local

# For remote production Cloudflare D1:
npm run db:migrate:remote
```

### 3. (Optional) Configure Cloudflare Queues for Paid Tier

If you are on the Workers Paid plan ($5/mo) and want dedicated queue buffering:
```bash
npx wrangler queues create koko-subs-queue
npx wrangler queues create koko-subs-dlq
```
Then add the `queues` block to `wrangler.jsonc`:
```jsonc
  "queues": {
    "producers": [
      {
        "binding": "SUBS_QUEUE",
        "queue": "koko-subs-queue"
      }
    ],
    "consumers": [
      {
        "queue": "koko-subs-queue",
        "max_batch_size": 10,
        "max_batch_timeout": 5
      }
    ]
  }
```
*Note: If you are on the Free plan, omit the `queues` block completely. The Hybrid Dispatcher will automatically route webhooks via direct background execution.*

### 4. Configure Secrets

Set production secrets via Wrangler CLI:

```bash
# 1. Meta Conversions API (CAPI)
npx wrangler secret put META_PIXEL_ID
npx wrangler secret put META_CAPI_ACCESS_TOKEN
npx wrangler secret put META_TEST_EVENT_CODE # Optional: e.g. TEST12345 for Events Manager

# 2. Google Analytics 4 (GA4 Measurement Protocol - Recommended)
npx wrangler secret put GOOGLE_ANALYTICS_MEASUREMENT_ID
npx wrangler secret put GOOGLE_ANALYTICS_API_SECRET

# 3. Google Ads API (Optional for direct GCLID offline conversion upload)
npx wrangler secret put GOOGLE_ADS_CUSTOMER_ID
npx wrangler secret put GOOGLE_ADS_CONVERSION_ACTION_ID
npx wrangler secret put GOOGLE_ADS_DEVELOPER_TOKEN
npx wrangler secret put GOOGLE_ADS_CLIENT_ID
npx wrangler secret put GOOGLE_ADS_CLIENT_SECRET
npx wrangler secret put GOOGLE_ADS_REFRESH_TOKEN

# 4. Webhook Authentication Secrets
npx wrangler secret put WEBHOOK_SECRET_REVENUECAT
npx wrangler secret put WEBHOOK_SECRET_ADAPTY
```

### 5. Local Development

Copy `.dev.vars.example` to `.dev.vars`:
```bash
cp .dev.vars.example .dev.vars
```
Fill in your test credentials in `.dev.vars`, then start the dev server:
```bash
npm run dev
```

### 6. Deploy to Production

```bash
npm run deploy
```

---

## Testing Webhooks & Endpoints

A complete test suite is available in `tests/mock-webhooks.http`, compatible with:
- VSCode **REST Client** extension (`humao.rest-client`)
- JetBrains IDEs Built-in HTTP Client
- `curl` commands (included in the comments)

### Test Scenarios Covered:
1. `GET /health` — Verifies worker is alive and reporting environment.
2. `POST /api/v1/attribution/sync` — Simulates client syncing `fbclid`, `gclid`, `fbp`, and `fbc`.
3. `POST /api/v1/webhooks/revenuecat` (`INITIAL_PURCHASE`) — Asserts record is marked `SKIPPED_CLIENT_HANDLED` in D1 `conversions` without redundant CAPI calls.
4. `POST /api/v1/webhooks/revenuecat` (`RENEWAL`) — Asserts event is deduplicated, dispatched to Meta CAPI and Google Ads in parallel, and recorded as `SENT`.
5. `POST /api/v1/webhooks/revenuecat` (`REFUND`) — Dispatches refund event to ad networks.
6. `POST /api/v1/webhooks/adapty` (`INITIAL_PURCHASE`) — Handles Adapty initial purchase.
7. `POST /api/v1/webhooks/adapty` (`SUBSCRIPTION_RENEWED`) — Handles Adapty renewal.
8. `POST /api/v1/webhooks/adapty` (`adapty_check_event: true`) — Webhook ping verification.

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
- **Response (Free Tier):**
  ```json
  {
    "ok": true,
    "logId": "2c51080a-5b1e-450a-8bf8-0902f2324900",
    "status": "dispatched_direct"
  }
  ```
- **Response (Paid Tier with Queues):**
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
- **Response:** Same status format as RevenueCat (`dispatched_direct` or `queued`).
