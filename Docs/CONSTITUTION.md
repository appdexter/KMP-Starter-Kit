# HIẾN PHÁP DỰ ÁN KOKO (KMP-CONTEST-STARTER-KIT)

> **Hiến chương Kiến trúc & Nguyên tắc Bất biến (Core Invariants)**
> Tài liệu này là kim chỉ nam tối cao cho toàn bộ quá trình phát triển, refactor và mở rộng hệ thống.

---

## 1. Nguyên Tắc Kiến Trúc Tối Cao (Architecture Invariants)
1. **MVP-First & Earned Simplicity:**
   - Ưu tiên tính tinh gọn, không pass-through UseCase nếu chỉ gọi thẳng Repository.
   - Chỉ tạo UseCase khi có: điều phối đa Repository, rule nghiệp vụ phức hợp, debounce/throttle, hoặc giao dịch logic đa luồng.
   - Concrete Repositories: Không tạo Interface dư thừa trừ khi có từ 2 implementations runtime trở lên (ngoại lệ: `subscription-api` đã có sẵn Adapty/RevenueCat).

2. **Wasm/Browser Target Safety (Bất khả xâm phạm):**
   - Tuyệt đối CẤM nhúng trực tiếp Firebase Client SDK (Firestore, Auth, Storage) vào `commonMain` làm vỡ build `wasmJs`.
   - Mọi kết nối Firebase/Cloud Functions hoặc backend Cloudflare phải bảo toàn tính đa nền tảng (Android, iOS, Desktop JVM, Web Wasm).

3. **Room 3 Single Source of Truth:**
   - Local Database là Room 3 chạy trên `commonMain` (`data/source/local/`).
   - Domain Model thuần túy không chứa `@Serializable` hay Room annotations.

---

## 2. Tracking, Monetization & Backend Invariants
1. **ROAS Tracking & Precision:**
   - Mọi luồng doanh thu (IAP, Subscriptions, AdMob ad_impression) phải được ghi nhận chuẩn xác đơn vị tiền tệ (`USD`, `VND`) và giá trị thực (vi mô / 1,000,000.0).
   - Hỗ trợ server-side conversion mapping để tối ưu chiến dịch tROAS trên Google Ads và Meta Ads.
2. **Cloudflare Edge First:**
   - Tận dụng Cloudflare Workers / D1 / Queues cho các tác vụ xử lý biên (Edge), Server-Side Tracking (CAPI, Measurement Protocol), và Webhooks từ Subscription Providers (RevenueCat / Adapty).
3. **Behavioral Psychology Onboarding:**
   - Luồng Onboarding không chỉ giới thiệu tính năng mà phải là phễu tâm lý: **Intent Capture ➔ Micro-Commitment ➔ Value Preview ➔ Permission Priming ➔ Tailored Paywall Peak**.

---

## 3. Quy Chuẩn Build, Test & Git
1. Không chạy lệnh native compiler trực tiếp (`gradlew`, `xcodebuild` thô bạo), tuân thủ scoped gates của dự án:
   - `spotlessCheck` / `spotlessApply`
   - `:shared:jvmTest` / `:shared:testAndroidHostTest`
   - `:androidApp:assembleDebug`
2. Mọi thay đổi kiến trúc hoặc tính năng phức tạp phải đi qua quy trình Swarm Research, Brainstorm, Spec Interview trước khi code.
