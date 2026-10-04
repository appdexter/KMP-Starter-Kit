# Mobile Measurement Partner (MMP) Tracking & Attribution Guide

> **Hệ Thống Đo Lường Chuyển Đổi & Attribution Đa Nền Tảng (Adjust & AppsFlyer)**
> Tương thích hoàn toàn với Kotlin Multiplatform (Android, iOS, Fallback Web/Desktop).

---

## 1. Tổng Quan Kiến Trúc (Architecture Overview)

Dự án cung cấp một kiến trúc MMP thống nhất, hỗ trợ cả **Adjust** và **AppsFlyer** mà **không làm phình to binary size** và **không phát sinh code thừa**.

Hệ thống hoạt động dựa trên 3 trụ cột:
1. **Dynamic Defensive Invocation:** SDK native (Adjust hoặc AppsFlyer) được kích hoạt linh hoạt qua cơ chế kiểm tra an toàn tại runtime. Không bao giờ gây crash ứng dụng nếu chưa cấu hình hoặc chạy trên môi trường test/web/desktop.
2. **Server-to-Server (S2S) Subscription Revenue:** Tuân thủ nguyên tắc không bắn sự kiện thanh toán gói đăng ký (In-App Purchases) trực tiếp từ client. Client chỉ truyền Device Attribution ID (`ADID` hoặc `AppsFlyer UID`) sang RevenueCat/Adapty để backend tự động gửi Webhook ghi nhận doanh thu chính xác (loại trừ trial hủy, chỉ tính tiền khi gia hạn thành công).
3. **AdMob Impression ROAS:** Tự động chặn sự kiện `ad_impression` từ AdMob thông qua `AnalyticsRouter` để chuyển đổi sang Ad Revenue tương ứng trên MMP nhằm tối ưu chiến dịch User Acquisition (tROAS).

---

## 2. Sơ Đồ Luồng Hoạt Động (Data Flow)

```
                       ┌─────────────────────────────────────┐
                       │       AppConfiguration.kt           │
                       │  MMP_PROVIDER = ADJUST | APPSFLYER  │
                       └──────────────────┬──────────────────┘
                                          │
                                          ▼
                       ┌─────────────────────────────────────┐
                       │        MmpTracker (Interface)       │
                       │  - initialize(config)               │
                       │  - getAttributionId(): String?      │
                       │  - getRevenueCatAttributeKey()      │
                       │  - trackAdRevenue(...)              │
                       └──────────────────┬──────────────────┘
                                          │
                ┌─────────────────────────┴─────────────────────────┐
                ▼                                                   ▼
┌───────────────────────────────┐                   ┌───────────────────────────────┐
│     AndroidAdjustTracker      │                   │   AndroidAppsFlyerTracker     │
│  - Trả về ADID                │                   │  - Trả về AppsFlyerUID        │
│  - Key: "$adjustId"           │                   │  - Key: "$appsflyerId"        │
└───────────────┬───────────────┘                   └───────────────┬───────────────┘
                │                                                   │
                └─────────────────────────┬─────────────────────────┘
                                          ▼
                       ┌─────────────────────────────────────┐
                       │      AnalyticsRouter (Multi-sink)   │
                       │   RevenueCat / Adapty S2S Webhook   │
                       └─────────────────────────────────────┘
```

---

## 3. Cấu Trúc Mã Nguồn (Code Structure)

```
shared/src/commonMain/kotlin/.../growth/analytics/mmp/
├── MmpProvider.kt               # Enum: NONE, ADJUST, APPSFLYER
├── MmpConfig.kt                 # Data class cấu hình token, môi trường, event mapping
├── MmpTracker.kt                # Interface trừu tượng đa nền tảng
├── NoImplMmpTracker.kt          # Stub an toàn cho Web (Wasm), Desktop (JVM), Unit Tests
└── MmpAnalyticsDestination.kt   # Adapter cắm vào AnalyticsRouter để fan-out sự kiện

shared/src/androidMain/kotlin/.../growth/analytics/mmp/
├── AndroidAdjustTracker.kt      # Native bridge an toàn cho Adjust Android SDK
├── AndroidAppsFlyerTracker.kt   # Native bridge an toàn cho AppsFlyer Android SDK
└── CompositeAndroidMmpTracker.kt# Bộ điều hướng tự động theo active provider

shared/src/iosMain/kotlin/.../
├── SwiftLibDependencyFactory.kt # Cầu nối interface sang native Swift trên iOS
└── Platform.ios.kt              # Koin module binding cho iOS
```

---

## 4. Hướng Dẫn Cấu Hình (Configuration)

Mọi cấu hình đều được tập trung tại **`AppConfiguration.kt`**:

### Khi dùng Adjust:
```kotlin
// 1. Chọn provider (hoặc để NONE, hệ thống tự nhận diện khi có token bên dưới)
val MMP_PROVIDER = MmpProvider.ADJUST

// 2. Điền token của ứng dụng (Adjust Dashboard -> App Settings)
const val ADJUST_APP_TOKEN_ANDROID = "akf25z3icn40"
const val ADJUST_APP_TOKEN_IOS = "boex4ggr8um8"

// 3. Mapping sự kiện in-app (Adjust Dashboard -> Events)
val ADJUST_EVENT_TOKENS = mapOf(
    "onboarding_completed" to "3q8ty5",
    "screen_view" to "nr0sx4",
)
```

### Khi dùng AppsFlyer:
```kotlin
// 1. Chọn provider
val MMP_PROVIDER = MmpProvider.APPSFLYER

// 2. Điền Dev Key và iOS App ID (AppsFlyer Dashboard -> App Settings)
const val APPSFLYER_DEV_KEY = "your_appsflyer_dev_key"
const val APPSFLYER_APP_ID_IOS = "123456789"

// 3. Mapping sự kiện in-app
val APPSFLYER_EVENT_TOKENS = mapOf(
    "onboarding_completed" to "af_complete_registration",
)
```

---

## 5. Quy Chuẩn Ghi Nhận Doanh Thu S2S (Server-to-Server)

1. **Tuyệt đối không bắn Purchase Event từ Client:**
   Việc bắn purchase event từ thiết bị sẽ gây ra hiện tượng **Double Counting** và ghi nhận doanh thu ảo (phantom revenue) cho các lượt dùng thử (Free Trial) mà người dùng hủy trước ngày thứ 7.
2. **Đồng bộ tự động qua Attribution ID:**
   Khi khởi động, `AppInitializer.initializeMmp()` tự động lấy ID attribution:
   - Với Adjust: Lấy `ADID` ➔ gửi lên RevenueCat qua attribute key `"$adjustId"`.
   - Với AppsFlyer: Lấy `AppsFlyer UID` ➔ gửi lên RevenueCat qua attribute key `"$appsflyerId"`.
3. **Cấu hình trên Dashboard RevenueCat:**
   - Vào **RevenueCat Dashboard ➔ Integrations ➔ Adjust** (hoặc **AppsFlyer**).
   - Bật integration và nhập App Token. RevenueCat sẽ tự động phân phối doanh thu thực tế về MMP.

---

## 6. Đo Lường Doanh Thu Quảng Cáo (Ad Revenue ROAS)

Khi bật `isAdRevenueTrackingEnabled = true` trong `MmpConfig`:
- `MmpAnalyticsDestination` tự động đón các sự kiện `Analytics.EVENT_AD_IMPRESSION` từ `AdsManager` (AdMob).
- Tự động chuẩn hóa đơn vị tiền tệ từ vi mô (`value_micros / 1,000,000.0`) sang giá trị thực (`Double`).
- Chuyển tiếp tới hàm `trackAdRevenue` của MMP để các mạng quảng cáo (Google Ads, Meta Ads, TikTok Ads) có dữ liệu tối ưu hóa tROAS.
