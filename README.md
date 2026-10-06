# KMP Application Platform

**KMP Application Platform** is a production-grade multiplatform architecture for building, scaling, and shipping high-performance applications across **Android**, **iOS**, and **Web (Wasm)** from a unified Kotlin codebase.

---

## 🏛️ Project Governance & Architecture Blueprint

Before building features or making architectural changes, review the foundational documentation:

- **[Hiến Pháp Kiến Trúc (Docs/CONSTITUTION.md)](Docs/CONSTITUTION.md)** — 7 điều khoản bất biến (MVP-first, Wasm safety, Room 3 local truth, ROAS precision, Edge backend, Native UX fidelity, Tiered quality gates).
- **[Kế Hoạch Sản Phẩm & PRD (Docs/MVP.md)](Docs/MVP.md)** — Đặc tả chi tiết về năng lực nền tảng, thiết kế dữ liệu, và lộ trình phát triển.
- **[MMP Tracking & Attribution (Docs/MMP_TRACKING.md)](Docs/MMP_TRACKING.md)** — Hướng dẫn cấu hình Adjust & AppsFlyer zero-overhead reflection, AdMob ROAS forwarding, và S2S In-App Purchase tracking.
- **[Cloudflare Edge Backend (Docs/CLOUDFLARE_BACKEND.md)](Docs/CLOUDFLARE_BACKEND.md)** — Kiến trúc backend biên Cloudflare Workers + D1 cho webhook, xử lý token, và Server-Side Conversion API (CAPI).

---

## 📂 Project Structure

```
├── MobileApp/         # Compose Multiplatform mobile app (Android, iOS, Web, Desktop)
│   ├── androidApp/    # Android application module
│   ├── webApp/        # Web (wasmJs) application module
│   ├── shared/        # Shared core business logic, Room 3 DB, Koin DI, UI screens
│   ├── designsystem/  # Shared design system composables and previews
│   └── iosApp/        # Xcode wrapper project (iosApp.xcodeproj)
├── Web/               # Landing page (Firebase Hosting) + Cloud Functions AI backend (Node.js)
├── Docs/              # Hiến pháp kiến trúc, PRD, MMP tracking, Cloudflare backend docs
├── AiGuidelines/      # Quy chuẩn kỹ thuật (Tech guidelines) và thiết kế vai trò (Role prompts)
├── skills/            # Vendor-neutral Agent Skills (SKILL.md) chuẩn hóa quy trình phát triển
├── .github/           # GitHub Actions CI/CD (pr_checks, publish Android Play Store, publish iOS)
└── AGENTS.md          # Sổ tay bối cảnh kỹ thuật trung tâm (Vendor-neutral Engineering Manual)
```

---

## ⚡ Tech Stack (Production Baseline)

- **Language**: Kotlin 2.4.10
- **UI Framework**: Compose Multiplatform 1.11.1
- **Platforms**: Android (minSdk 24, compileSdk 37), iOS (16.0+), Web (Wasm), JVM Desktop
- **Build System**: AGP 9.3.1, Gradle 9.7.1, Gradle Kotlin DSL
- **Dependency Injection**: Koin 4.2.2
- **Networking**: Ktor 3.5.2 (Content Negotiation, Kotlinx Serialization, Auth, Logging)
- **Local Persistence**: Room 3.0.1 (KMP — `sqlite-bundled` on Mobile/Desktop, `sqlite-web` + OPFS on Wasm)
- **Key-Value Storage**: Jetpack DataStore Preferences 1.3.0-alpha10 (All targets including Web)
- **Permissions**: Calf 0.13.0 (Camera, Gallery, Notification, Location)
- **Authentication**: Firebase Authentication + Social Sign-In (Google, Apple, Anonymous)
- **Monetization**: Adapty 3.17.0 (default) / RevenueCat 3.5.1 + Google AdMob 25.4.0 (with ILR ROAS tracking)
- **Analytics & Messaging**: Firebase BOM 34.18.0 (Analytics, Crashlytics, Remote Config, FCM Messaging)
- **Quality Gates**: Spotless 8.10.0 + ktlint 1.8.0, Roborazzi 1.72.0 + ComposablePreviewScanner 0.9.3

---

## 🚀 Getting Started

### 1. Prerequisites
- **JDK 17+** (JDK 21 recommended)
- **Android Studio** (latest stable release) with Android SDK API 34/35+
- **Xcode** (for iOS compilation and simulator runs)
- Optional: Verify your environment with [KDoctor](https://github.com/Kotlin/kdoctor)

### 2. Environment Configuration
Copy the template configuration file in `MobileApp/`:
```bash
cp MobileApp/local.properties.example MobileApp/local.properties
```
Add your Android SDK path and optional API keys (Firebase, AdMob, Adapty/RevenueCat).

### 3. Run the Application
All Gradle commands run from the `MobileApp/` directory:

```bash
cd MobileApp

# Build & Run Android Debug APK
./gradlew :androidApp:assembleDebug

# Run Web/Wasm Development Server (DevServer with hot reload)
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# Preview Design System Components (Desktop JVM)
# Run designsystem/src/jvmMain/kotlin/Main.kt from your IDE
```

For iOS: Open `MobileApp/iosApp/iosApp.xcodeproj` in Xcode and select your target simulator or device.

---

## 🛠️ Quality Gates & Verification

Every code change must pass the three scoped quality gates before commit or PR:

```bash
cd MobileApp

# 1. Code formatting & lint
./gradlew spotlessCheck
# Auto-fix lint issues:
./gradlew spotlessApply

# 2. Shared Unit & Headless Compose UI tests
./gradlew :shared:jvmTest :shared:testAndroidHostTest

# 3. Android Debug Compilation (validates :shared transitively)
./gradlew :androidApp:assembleDebug
```

To capture snapshot previews of UI screens locally (powered by Roborazzi):
```bash
./gradlew :shared:recordRoborazziAndroidHostTest
```

---

## 🧭 Standard Operating Procedures (Agent Skills)

Quy trình phát triển và vận hành sản phẩm được chuẩn hóa thành các **Task Skills** độc lập tại [`skills/`](skills/README.md), có thể thực thi tự động bởi AI Coding Agents hoặc thực hiện thủ công:

- **Kiến trúc & UX**: [`kmp-product-engineer`](skills/kmp-product-engineer/SKILL.md) — Chuẩn hóa state modeling (`ScreenUiState`), Strong Skipping, và UX platform fidelity (iOS swipe-back, Android edge-to-edge).
- **Tính năng mới**: [`new-screen`](skills/new-screen/SKILL.md), [`new-local-model`](skills/new-local-model/SKILL.md), [`add-api-service`](skills/add-api-service/SKILL.md), [`save-preferences`](skills/save-preferences/SKILL.md).
- **Thương mại hóa & ROAS**: [`paywall-upgrade-cro`](skills/paywall-upgrade-cro/SKILL.md), [`setup-subscriptions`](skills/setup-subscriptions/SKILL.md), [`admob-roas`](skills/admob-roas/SKILL.md).
- **Tiền kiểm định & Phát hành**: [`audit-store-readiness`](skills/audit-store-readiness/SKILL.md), [`optimize-store-metadata`](skills/optimize-store-metadata/SKILL.md), [`publish-release`](skills/publish-release/SKILL.md).

Chi tiết toàn bộ quy trình và mục lục tra cứu: xem [`skills/README.md`](skills/README.md) và [`AGENTS.md`](AGENTS.md).

---

## 📄 License

Released under the [MIT License](LICENSE) — Copyright (c) 2026.
