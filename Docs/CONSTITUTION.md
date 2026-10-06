# HIẾN PHÁP KIẾN TRÚC DỰ ÁN (PROJECT CONSTITUTION TEMPLATE)

> **Dự án:** {{PROJECT_NAME}}  
> **Package ID:** {{PACKAGE_ID}}  
> **Mục tiêu sản phẩm:** {{PRODUCT_MISSION}}  
> **Nền tảng mục tiêu:** Android, iOS, Web (Wasm), Desktop (JVM)  
> **Trạng thái:** BẮT BUỘC TUÂN THỦ (IMMUTABLE INVARIANTS)

---

## MỤC ĐÍCH & PHẠM VI ÁP DỤNG

Tài liệu này là **Hiến chương Kiến trúc & Nguyên tắc Bất biến** tối cao của dự án. Mọi kỹ sư, lập trình viên và AI coding agent tham gia phát triển đều phải tuân thủ nghiêm ngặt 7 Điều khoản dưới đây. Không một quyết định kỹ thuật nào được phép vi phạm Hiến pháp này trừ khi có sự phê chuẩn trực tiếp bằng văn bản từ Lead Architect.

---

## ĐIỀU 1: NGUYÊN TẮC TINH GỌN (MVP-FIRST & EARNED SIMPLICITY)

1. **Kiến trúc 3 tầng chuẩn:**
   ```
   Data Layer (Room DAO / Ktor ApiService / Repository)
        │
        ▼
   ViewModel Layer (StateFlow<ScreenUiState>, Event Handlers)
        │
        ▼
   Presentation Layer (Dual-Overload Stateless Screen Composables)
   ```
2. **CẤM UseCase 1 tầng thừa (Ban Pass-through UseCases):**
   - Chỉ tạo UseCase khi có từ 2 Repository trở lên, hoặc cần điều phối transaction logic, debounce/throttle, hoặc rule nghiệp vụ phức hợp. Gọi thẳng Repository từ ViewModel cho 90% trường hợp thông thường.
3. **Concrete Repositories:**
   - Ưu tiên class cụ thể, không tạo Interface trừ khi tồn tại từ 2 implementations runtime trở lên (ví dụ: `subscription-api` có Adapty và RevenueCat).

---

## ĐIỀU 2: AN TOÀN NỀN TẢNG WEB / WASM (INVIOLABLE WASM SAFETY)

1. **Cấm SDK phá vỡ Wasm vào `commonMain`:**
   - Tuyệt đối CẤM nhúng trực tiếp Firebase Client SDK (Firestore, Auth, Storage) vào `commonMain` làm vỡ build target `wasmJs`.
2. **Kiến trúc đám mây trung lập:**
   - Dữ liệu đồng bộ đám mây và AI Transport phải đi qua Cloud Functions AI Proxy hoặc Cloudflare Edge API với định dạng JSON chuẩn.

---

## ĐIỀU 3: NGUỒN CHÂN LÝ DỮ LIỆU CỤC BỘ (LOCAL SOURCE OF TRUTH)

1. **Room 3 Single Source of Truth:**
   - Toàn bộ dữ liệu nghiệp vụ offline-first lưu trữ tại Room 3 trên `commonMain` (`data/source/local/`).
   - Sử dụng `sqlite-bundled` cho Android, iOS, JVM và `WebWorkerSQLiteDriver + OPFS` cho Web Wasm.
2. **Domain Model thuần khiết:**
   - Domain Model là immutable `data class`, không chứa Room annotations, không chứa `@Serializable`.
   - DTOs và Room Entities chỉ làm nhiệm vụ truyền tải và phải map sang Domain Model trước khi lên UI.

---

## ĐIỀU 4: CHUẨN XÁC DOANH THU & THEO DÕI TĂNG TRƯỞNG (ROAS PRECISION)

1. **Bắt buộc Impression-Level Ad Revenue (ILR):**
   - Mọi định dạng quảng cáo AdMob (Banner, Interstitial, Rewarded) phải gắn `OnPaidEventListener` (Android) và `paidEventHandler` (iOS) để ghi nhận sự kiện `ad_impression` với giá trị vi mô (`valueMicros / 1,000,000.0`) đẩy lên Firebase Analytics cho chiến dịch Google Ads tROAS.
2. **Attribution & S2S Purchase Tracking:**
   - Hỗ trợ MMP (Adjust / AppsFlyer) qua Reflection phòng thủ (Defensive Reflection).
   - In-App Purchase và Subscriptions ưu tiên mô hình Server-to-Server (S2S) tránh trùng lặp doanh thu.

---

## ĐIỀU 5: HẠ TẦNG BIÊN KẾT HỢP ĐÁM MÂY (EDGE-FIRST BACKEND)

1. **Cloudflare Edge First:**
   - Tận dụng Cloudflare Workers + D1 cho các tác vụ xử lý biên: Webhooks, Server-Side Tracking (Meta CAPI, Measurement Protocol) và xác thực trung gian.
2. **Firebase Cloud Ecosystem:**
   - Đảm nhiệm Authentication (Google, Apple, Anonymous) và Cloud Messaging (FCM Push Notifications).

---

## ĐIỀU 6: TRẢI NGHIỆM NGƯỜI DÙNG NATIVE (PLATFORM UX FIDELITY)

1. **Tôn trọng Muscle Memory của từng nền tảng:**
   - **iOS:** Interactive edge swipe-back, Safe Area & Dynamic Island insets, keyboard dismissal khi chạm bên ngoài.
   - **Android:** Android 15+ edge-to-edge bắt buộc, IME keyboard insets, Predictive Back Animation.
2. **Thiết kế State là thiết kế UX:**
   - Mọi màn hình phải có Sealed Interface `ScreenUiState` (Loading, Content, Empty, Error). CẤM boolean soup (`val isLoading: Boolean`, `val isError: Boolean`).
3. **Mẫu Dual-Overload Composable:**
   - Mỗi màn hình bắt buộc có overload nhận ViewModel (điều hướng) và overload nhận pure `uiState` + `onUiEvent` (phục vụ instant `@Preview` và headless Compose tests).

---

## ĐIỀU 7: CỔNG KIỂM ĐỊNH TƯƠNG XỨNG & KỶ LUẬT BUILD (QUALITY GATES)

1. **Không chạy compiler native bừa bãi:**
   - Tuyệt đối không chạy full native build (`./gradlew check`, `clean build`, `xcodebuild`) cho các thay đổi về tài liệu (Cấp 0) hoặc UI cosmetic (Cấp 1).
2. **3 Cổng kiểm tra Scoped bắt buộc trước PR/Commit:**
   - Cổng 1 (Lint): `./gradlew spotlessCheck` (tự sửa: `./gradlew spotlessApply`).
   - Cổng 2 (Logic & Compose UI): `./gradlew :shared:jvmTest :shared:testAndroidHostTest`.
   - Cổng 3 (Android Debug Build): `./gradlew :androidApp:assembleDebug`.

---

## HƯỚNG DẪN KÍCH HOẠT TEMPLATE CHO DỰ ÁN MỚI

Khi khởi tạo một sản phẩm mới từ nền tảng này:
1. Sao chép template này và thay thế các biến `{{PROJECT_NAME}}`, `{{PACKAGE_ID}}`, `{{PRODUCT_MISSION}}` bằng thông tin sản phẩm thực tế.
2. Đặt file tại `Docs/CONSTITUTION.md` của dự án.
3. Cam kết toàn bộ thành viên dự án và AI coding agents tuân thủ nghiêm ngặt 7 điều khoản trên.
