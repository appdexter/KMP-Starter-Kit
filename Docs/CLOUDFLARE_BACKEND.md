# Tài Liệu Kỹ Thuật: Cloudflare Edge Backend (Koko Starter Kit)

> **Dự án:** Koko (KMP Contest Starter Kit)  
> **Module:** `backend/cloudflare/`  
> **Môi trường Live:** `https://koko-backend.koko-kmp-backend.workers.dev`  
> **Cloudflare Account ID:** `da0e99770e4d3623b4d5665d01b52084` (`KPM Kit`)  
> **Database:** Cloudflare D1 (`koko-db` - ID: `3f168b91-a061-4406-8966-3f9198e56649`)

---

## 1. Giới Thiệu & Mục Tiêu Kiến Trúc

Trong mô hình ứng dụng di động hiện đại (Subscription & Ad-supported), việc đo lường hiệu quả quảng cáo (ROAS) từ Meta Ads và Google Ads gặp thách thức lớn do cơ chế bảo vệ quyền riêng tư trên thiết bị (iOS ATT, Android Sandbox, Cookie deprecation trên Web).

**Cloudflare Edge Backend** của Koko giải quyết triệt để vấn đề này bằng cách:
1. **Server-Side Attribution Sync:** Lưu trữ định danh quảng cáo (`fbclid`, `gclid`, `_fbp`, `_fbc`, IP, User-Agent) từ client app lên Edge Database (Cloudflare D1) ngay khi user khởi chạy app.
2. **Server-Side Conversion Dispatch (CAPI):** Lắng nghe webhook vòng đời gói cước (In-App Purchase / Subscription) từ **RevenueCat** hoặc **Adapty**. Khi có gia hạn gói (`RENEWAL`) hoặc hoàn tiền (`REFUND`), backend tự động tổng hợp dữ liệu định danh và bắn trực tiếp sang **Meta Conversions API (CAPI v20.0)** và **Google Ads Offline Conversion / GA4 Measurement Protocol**.
3. **Zero Double-Counting:** Tự động nhận diện và bỏ qua các sự kiện mua lần đầu (`INITIAL_PURCHASE`) vốn đã được SDK trên client bắn, chỉ ghi log audit chứ không bắn CAPI để chống nhân đôi doanh thu.
4. **Hybrid Execution (Zero-Cost First):** Hoạt động mượt mà 100% trên **Cloudflare Free Tier ($0/tháng)** bằng cơ chế `c.executionCtx.waitUntil(...)`, và tự động chuyển sang **Cloudflare Queues** khi nâng cấp gói Paid ($5/tháng) mà không cần sửa code.

---

## 2. Sơ Đồ Kiến Trúc Hoạt Động (Architecture Flow)

```
┌────────────────────────────────────────────────────────────────────────┐
│             KMP Client (Android / iOS / Desktop JVM / Web Wasm)        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    │ 1. Deep Link / Cookie Capture
                                    │    (fbclid, gclid, _fbp, _fbc)
                                    ▼
                ┌───────────────────────────────────────┐
                │       AttributionRepository.kt        │
                │     POST /api/v1/attribution/sync     │
                └───────────────────┬───────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Cloudflare Edge Worker                          │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ POST /api/v1/attribution/sync ➔ Lưu vào Cloudflare D1 (users)  │   │
│   └────────────────────────────────────────────────────────────────┘   │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ POST /api/v1/webhooks/{revenuecat, adapty}                     │   │
│   │ ➔ D1 (webhooks_log)                                            │   │
│   │ ➔ Hybrid Dispatcher:                                           │   │
│   │    ├─ Free Tier ($0): c.executionCtx.waitUntil(processEvent)   │   │
│   │    └─ Paid Tier ($5): env.SUBS_QUEUE.send(job)                 │   │
│   └───────────────────────────────┬────────────────────────────────┘   │
│                                   │                                    │
│                                   ▼                                    │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ Event Classifier & Deduplication Engine                        │   │
│   │ ├─ INITIAL_PURCHASE: Lưu audit D1, KHÔNG bắn CAPI (chống lặp)   │   │
│   │ └─ RENEWAL, PRODUCT_CHANGE, REFUND:                            │   │
│   │     ➔ Check D1 (conversions) deduplication                     │   │
│   │     ➔ Meta CAPI (Graph API v20.0 SHA-256)                      │   │
│   │     ➔ Google Ads Conversion / GA4 Measurement Protocol         │   │
│   └────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Cơ Sở Dữ Liệu Cloudflare D1 (`koko-db`)

Database D1 sử dụng SQLite Serverless phân tán toàn cầu. Schema định nghĩa tại [schema.sql](file:///Users/trungkientn/Dev2/KotlinM/kmp-contest-starter-kit/backend/cloudflare/src/db/schema.sql).

### 3.1 Bảng `users`
Lưu trữ thông tin liên kết giữa `app_user_id` (ID người dùng trong app / RevenueCat / Adapty) và các định danh quảng cáo:

| Cột | Kiểu | Mô tả |
| :--- | :--- | :--- |
| `id` | `TEXT PRIMARY KEY` | UUID bản ghi |
| `app_user_id` | `TEXT NOT NULL UNIQUE` | Định danh người dùng duy nhất |
| `fbclid` | `TEXT` | Facebook Click ID từ deep link |
| `gclid` | `TEXT` | Google Click ID từ deep link |
| `fbp` | `TEXT` | Facebook Browser ID (`_fbp` cookie) |
| `fbc` | `TEXT` | Facebook Click Cookie (`_fbc` cookie) |
| `ip_address` | `TEXT` | Địa chỉ IP của client (fallback từ header CF) |
| `user_agent` | `TEXT` | User Agent của thiết bị |
| `created_at` | `INTEGER NOT NULL` | Thời gian tạo (epoch millis) |
| `updated_at` | `INTEGER NOT NULL` | Thời gian cập nhật gần nhất |

### 3.2 Bảng `webhooks_log`
Lưu vết toàn bộ webhook nhận được từ RevenueCat hoặc Adapty phục vụ việc audit và replay khi cần:

| Cột | Kiểu | Mô tả |
| :--- | :--- | :--- |
| `id` | `TEXT PRIMARY KEY` | Transaction ID hoặc Webhook Event ID |
| `provider` | `TEXT NOT NULL` | `'revenuecat'` hoặc `'adapty'` |
| `event_type` | `TEXT NOT NULL` | Loại sự kiện gốc (ví dụ: `RENEWAL`, `INITIAL_PURCHASE`) |
| `payload` | `TEXT NOT NULL` | Toàn bộ nội dung JSON gốc |
| `status` | `TEXT NOT NULL` | `RECEIVED`, `QUEUED`, `PROCESSING_DIRECT`, `PROCESSED`, `FAILED`, `IGNORED` |
| `error` | `TEXT` | Thông báo lỗi nếu xử lý thất bại |
| `created_at` | `INTEGER NOT NULL` | Thời điểm ghi nhận |

### 3.3 Bảng `conversions`
Lưu trữ các chuyển đổi đã dispatch sang Meta CAPI và Google Ads để đảm bảo tính **Idempotency (chống bắn trùng lặp)**:

| Cột | Kiểu | Mô tả |
| :--- | :--- | :--- |
| `id` | `TEXT PRIMARY KEY` | UUID bản ghi |
| `user_id` | `TEXT NOT NULL` | Liên kết tới `users.app_user_id` |
| `event_name` | `TEXT NOT NULL` | Tên sự kiện (ví dụ: `Purchase`, `Refund`) |
| `value` | `REAL` | Doanh thu thực tế (đơn vị tiền tệ gốc) |
| `currency` | `TEXT` | Mã tiền tệ ISO (ví dụ: `USD`, `VND`) |
| `event_id` | `TEXT NOT NULL UNIQUE` | Khóa duy nhất chống trùng (ví dụ: `meta_{txId}_purchase`) |
| `status` | `TEXT NOT NULL` | `SENT`, `FAILED`, `SKIPPED_DEDUPE`, `SKIPPED_CLIENT_HANDLED` |
| `error` | `TEXT` | Chi tiết lỗi từ Meta Graph API hoặc Google Ads API |
| `created_at` | `INTEGER NOT NULL` | Thời điểm ghi nhận |

---

## 4. Danh Sách API Endpoints

### 4.1 Health Check
* **Method:** `GET`
* **Path:** `/health`
* **Response (200 OK):**
```json
{
  "status": "ok",
  "service": "koko-backend",
  "timestamp": 1791186752131,
  "environment": "production"
}
```

### 4.2 Đồng Bộ Attribution
* **Method:** `POST`
* **Path:** `/api/v1/attribution/sync`
* **Headers:** `Content-Type: application/json`
* **Request Body:**
```json
{
  "app_user_id": "usr_987654321",
  "fbclid": "fb.1.1710000000.IwAR_sample_fbclid",
  "gclid": "sample_gclid_12345",
  "fbp": "fb.1.1710000000.99999999",
  "fbc": "fb.1.1710000000.IwAR_sample_fbclid",
  "ip_address": "123.45.67.89",
  "user_agent": "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4)..."
}
```
* **Response (200 OK):**
```json
{
  "ok": true,
  "message": "Attribution identifiers synced successfully",
  "app_user_id": "usr_987654321"
}
```
* **Ghi chú:** Nếu client không truyền `ip_address` hoặc `user_agent`, Worker sẽ tự động trích xuất từ Cloudflare request headers (`cf-connecting-ip` và `user-agent`).

### 4.3 Webhook RevenueCat
* **Method:** `POST`
* **Path:** `/api/v1/webhooks/revenuecat`
* **Headers:**
  * `Authorization: Bearer <WEBHOOK_SECRET_REVENUECAT>` (hoặc header `x-revenuecat-secret`)
  * `Content-Type: application/json`
* **Response (200 OK):**
```json
{
  "ok": true,
  "status": "dispatched_direct",
  "event_id": "rc_evt_123456",
  "event_type": "RENEWAL"
}
```

### 4.4 Webhook Adapty
* **Method:** `POST`
* **Path:** `/api/v1/webhooks/adapty`
* **Headers:**
  * `Authorization: Bearer <WEBHOOK_SECRET_ADAPTY>` (hoặc header `x-adapty-secret`)
  * `Content-Type: application/json`
* **Response (200 OK):**
```json
{
  "ok": true,
  "status": "dispatched_direct",
  "transaction_id": "adp_tx_98765",
  "event_type": "SUBSCRIPTION_RENEWED"
}
```

---

## 5. Cơ Chế Xử Lý Sự Kiện & Chống Trùng Lặp

### 5.1 Bảng Phân Loại Sự Kiện (Event Classification)

| Nhà cung cấp | Loại sự kiện gốc | Phân loại Backend | Hành động Server CAPI / Google Ads |
| :--- | :--- | :--- | :--- |
| **RevenueCat** | `INITIAL_PURCHASE` | `SKIP_CLIENT_HANDLED` | Lưu D1 với status `SKIPPED_CLIENT_HANDLED`, **không bắn CAPI** |
| **RevenueCat** | `RENEWAL`, `NON_RENEWING_PURCHASE`, `PRODUCT_CHANGE` | `SEND_PURCHASE` | Khử trùng lặp qua D1 ➔ **Bắn Purchase CAPI & Google Ads** |
| **RevenueCat** | `REFUND`, `REVOCATION` | `SEND_REFUND` | Khử trùng lặp qua D1 ➔ **Bắn Refund CAPI & Google Ads** |
| **Adapty** | `INITIAL_PURCHASE`, `SUBSCRIPTION_STARTED` | `SKIP_CLIENT_HANDLED` | Lưu D1 với status `SKIPPED_CLIENT_HANDLED`, **không bắn CAPI** |
| **Adapty** | `SUBSCRIPTION_RENEWED`, `RENEWAL` | `SEND_PURCHASE` | Khử trùng lặp qua D1 ➔ **Bắn Purchase CAPI & Google Ads** |
| **Adapty** | `SUBSCRIPTION_REFUNDED`, `REFUND` | `SEND_REFUND` | Khử trùng lặp qua D1 ➔ **Bắn Refund CAPI & Google Ads** |

### 5.2 Khử Trùng Lặp (Deduplication Logic)
Mỗi sự kiện chuyển đổi tạo ra một `event_id` chuẩn hoá:
* Meta Purchase: `meta_{transactionId}_purchase`
* Meta Refund: `meta_{transactionId}_refund`
* Google Ads: `gads_{transactionId}_{timestamp}`

Trước khi dispatch, Worker truy vấn `conversions` theo `event_id`. Nếu đã tồn tại bản ghi với trạng thái `SENT`, sự kiện bị bỏ qua ngay lập tức, ngăn chặn việc tính trùng doanh thu khi webhook được gửi lại (retry).

---

## 6. Tích Hợp KMP Client (`MobileApp/shared/`)

Client app trong Kotlin Multiplatform được tích hợp sẵn sàng không cần code thêm boilerplate:

### 6.1 Cấu Hình Base URL
Trong [AppConfiguration.kt](file:///Users/trungkientn/Dev2/KotlinM/kmp-contest-starter-kit/MobileApp/shared/src/commonMain/kotlin/com/kotlinfoundation/koko/root/AppConfiguration.kt#L90):
```kotlin
const val CLOUDFLARE_BACKEND_URL = "https://koko-backend.koko-kmp-backend.workers.dev"
```

### 6.2 Sử Dụng Trong Code Feature / Launch Flow
Inject `AttributionRepository` qua Koin:
```kotlin
class DeepLinkHandler(
    private val attributionRepository: AttributionRepository
) {
    fun onDeepLinkReceived(url: String) {
        val uri = parseUri(url)
        val fbclid = uri.getQueryParameter("fbclid")
        val gclid = uri.getQueryParameter("gclid")
        
        // Cache lại tham số attribution
        attributionRepository.setAttributionParams(
            fbclid = fbclid,
            gclid = gclid,
        )
    }

    suspend fun onUserLoggedIn(userId: String) {
        // Đồng bộ dữ liệu lên Cloudflare Edge Backend
        val result = attributionRepository.sync(appUserId = userId)
        result.onSuccess { response ->
            AppLogger.d("Đồng bộ attribution thành công: ${response.message}")
        }
    }
}
```

---

## 7. Hướng Dẫn Vận Hành & Thiết Lập Secrets

### 7.1 Cấu Hình Webhook Trên Dashboard Nhà Cung Cấp

#### RevenueCat Dashboard:
1. Vào **Project Settings ➔ Integrations ➔ Webhooks**.
2. Thêm Webhook URL:
   ```
   https://koko-backend.koko-kmp-backend.workers.dev/api/v1/webhooks/revenuecat
   ```
3. Đặt **Authorization Header**: `Bearer <CHỌN_MỘT_SECRET_NGẪU_NHIÊN>`.
4. Bật các sự kiện: `Initial Purchase`, `Renewal`, `Product Change`, `Cancellation`, `Billing Issue`.

#### Adapty Dashboard:
1. Vào **App Settings ➔ Integrations ➔ Webhooks**.
2. Thêm Webhook URL:
   ```
   https://koko-backend.koko-kmp-backend.workers.dev/api/v1/webhooks/adapty
   ```
3. Đặt Secret Token tương tự.

### 7.2 Nạp Secrets Cho Cloudflare Worker
Chạy các lệnh sau từ thư mục gốc của project:

```bash
# Webhook Authentication Secrets
CLOUDFLARE_ACCOUNT_ID=da0e99770e4d3623b4d5665d01b52084 npx --prefix backend/cloudflare wrangler secret put WEBHOOK_SECRET_REVENUECAT
CLOUDFLARE_ACCOUNT_ID=da0e99770e4d3623b4d5665d01b52084 npx --prefix backend/cloudflare wrangler secret put WEBHOOK_SECRET_ADAPTY

# Meta Conversions API
CLOUDFLARE_ACCOUNT_ID=da0e99770e4d3623b4d5665d01b52084 npx --prefix backend/cloudflare wrangler secret put META_PIXEL_ID
CLOUDFLARE_ACCOUNT_ID=da0e99770e4d3623b4d5665d01b52084 npx --prefix backend/cloudflare wrangler secret put META_CAPI_ACCESS_TOKEN
# (Tuỳ chọn) Test Event Code từ Meta Events Manager để test realtime
CLOUDFLARE_ACCOUNT_ID=da0e99770e4d3623b4d5665d01b52084 npx --prefix backend/cloudflare wrangler secret put META_TEST_EVENT_CODE

# Google Ads / GA4 Measurement Protocol
CLOUDFLARE_ACCOUNT_ID=da0e99770e4d3623b4d5665d01b52084 npx --prefix backend/cloudflare wrangler secret put GOOGLE_ANALYTICS_MEASUREMENT_ID
CLOUDFLARE_ACCOUNT_ID=da0e99770e4d3623b4d5665d01b52084 npx --prefix backend/cloudflare wrangler secret put GOOGLE_ANALYTICS_API_SECRET
```

### 7.3 Kiểm Thử Cục Bộ & Giả Lập Webhook
Project đi kèm file [mock-webhooks.http](file:///Users/trungkientn/Dev2/KotlinM/kmp-contest-starter-kit/backend/cloudflare/tests/mock-webhooks.http) chứa đầy đủ payload mẫu:
* Mở bằng tiện ích **REST Client** trên VSCode hoặc **HTTP Client** trên IntelliJ/Android Studio.
* Hoặc chạy trực tiếp qua `curl` theo hướng dẫn trong file.
