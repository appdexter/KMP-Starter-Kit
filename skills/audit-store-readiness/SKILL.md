---
name: audit-store-readiness
description: >-
  Pre-flight review audit for App Store and Google Play submissions — scans codebase, Info.plist,
  paywall compliance (Restore Purchases, EULA, Privacy links), Privacy Manifests, and Google Play Data
  Safety requirements before cutting a release. Use before `publish-release` or whenever asking "is my
  app ready for App Store / Google Play review?". Part of the `publishing` phase.
---

# Audit Store Readiness (Pre-Flight Review Gate)

This skill performs an automated and manual pre-flight audit of the application against **Apple App Store
Review Guidelines** and **Google Play Developer Policies**. Running this audit before uploading builds
prevents costly rejections and review delays.

---

## 1. Quick Environment & Secret Check

Run the built-in environment audit from `MobileApp/`:

```bash
cd MobileApp
./scripts/check_env.sh --phase publishing
```

Verify that:
- [ ] No `testValue` placeholders exist for production service keys.
- [ ] Release signing keystores and certificates are properly configured via `setup-signing`.
- [ ] App versioning is properly incremented via `bump-version`.

---

## 2. Apple App Store Compliance Audit

### A. Guideline 3.1.1 — In-App Purchases & Subscriptions
If the app includes subscriptions, in-app purchases, or credit packs (Phase 4):
- [ ] **Restore Purchases button**: A visible, functional "Restore Purchases" button **must** exist on the Paywall and in the App Settings screen.
- [ ] **Terms of Use & Privacy Policy links**: Direct, clickable links to your **Terms of Use (EULA)** and **Privacy Policy** must appear on the paywall screen itself.
  - Apple standard EULA link or custom terms URL.
- [ ] **Transparent Pricing**: The paywall must explicitly state:
  - Exact price and currency.
  - Billing period (e.g. *"$4.99 per month"* or *"$29.99 per year after 7-day free trial"*).
  - Renewal disclosure: *"Subscription automatically renews unless auto-renew is turned off at least 24 hours before the end of the current period."*
- [ ] Check string definitions in `composeResources/values/strings.xml`:
  Search for `paywall_sub_disclosure`, `terms_of_service`, `privacy_policy`.

### B. Guideline 5.1.1 — Privacy & Privacy Manifests
- [ ] **Privacy Manifest (`PrivacyInfo.xcprivacy`)**:
  Inspect `iosApp/iosApp/PrivacyInfo.xcprivacy` (or linked framework). Verify declared data types match what your app and SDKs (Firebase Analytics, Crashlytics, Adapty, AdMob) collect.
- [ ] **Permission Descriptions (`Info.plist`)**:
  If using permissions (Camera, Microphone, Photo Library, Notifications):
  Inspect `iosApp/iosApp/Info.plist`. The usage description strings (e.g. `NSCameraUsageDescription`) must specify **why** the app needs the feature:
  - ❌ BAD: *"This app needs camera access."* (Auto-rejected by Apple)
  - ✅ GOOD: *"Camera access is required to take profile photos and scan QR codes."*
- [ ] **App Tracking Transparency (ATT)**:
  If using AdMob with tracking enabled, `NSUserTrackingUsageDescription` must be declared and the ATT prompt presented before ad loading.

### C. Guideline 2.1 — App Completeness & Crash on Launch
- [ ] No placeholder content: Check for `"Lorem Ipsum"`, `"Coming Soon"`, or disabled stub buttons that lead to nowhere.
- [ ] Verify iOS builds cleanly without memory corruption or missing architecture slices:
  `./gradlew :shared:integrateEmbedAndSign`

### D. Guideline 5.1.1(v) — Account Deletion Requirement
- [ ] If your app offers sign-in (`enable-auth` via Apple / Google), you **must** provide a straightforward way for users to **delete their account and associated data** from within the app settings.

---

## 3. Google Play Store Compliance Audit

### A. Target API & Core Requirements
- [ ] App targets the required API level (Android 14/15, API 34+). Verified in `build.gradle.kts` (`targetSdk = 35`).
- [ ] 64-bit architecture supported (standard in KMP).

### B. Data Safety Form Alignment
- [ ] Verify declared data types in Google Play Console match actual SDK collection:
  - **Firebase Analytics / Crashlytics**: Crash logs, diagnostics, app interactions.
  - **AdMob**: Device IDs, coarse location, ad interactions.
  - **Adapty / RevenueCat**: Purchase history, user identifiers.
- [ ] If collecting personal info, the Privacy Policy URL must be valid and publicly accessible without authentication.

### C. Ads Policy & Deceptive Behavior
- [ ] **No cold launch interstitials**: Do not display full-screen interstitial ads immediately when opening the app or before user interaction.
- [ ] **Dismissible ads**: Close buttons on interstitials and rewarded ads must be easily tap-able and not obscured by UI bars.

---

## 4. Automated Code & Asset Scan

Run this rapid scan from `MobileApp/`:

```bash
# 1. Check for unreplaced placeholder strings in Compose resources
grep -rn "TODO" shared/src/commonMain/composeResources/values/
grep -rn "Lorem" shared/src/commonMain/composeResources/values/

# 2. Check for missing permission strings in iOS Info.plist
grep -E "UsageDescription" iosApp/iosApp/Info.plist

# 3. Verify quality gates pass
./gradlew spotlessCheck :shared:jvmTest :shared:testAndroidHostTest :androidApp:assembleDebug
```

---

## 5. Audit Report Checklist

Fill out this summary before proceeding to `publish-release`:

```markdown
### Store Readiness Review Summary
- [ ] Environment & Keys: All production keys set in local.properties / CI secrets
- [ ] Restore Purchases: Tested and verified functional on Paywall & Settings
- [ ] Terms & Privacy: Clickable links verified on Paywall
- [ ] iOS PrivacyInfo.xcprivacy: Present and covers all active SDKs
- [ ] iOS Info.plist: All usage descriptions provide clear justification
- [ ] Account Deletion: Accessible if social auth is enabled
- [ ] Quality Gates: Spotless, JVM tests, Host tests, and AssembleDebug green
```
