# PRD — KMP App Starter Platform

**Version:** 1.0  
**Status:** Draft  
**Platform:** Kotlin Multiplatform — Android + iOS  
**Primary Backend:** Cloudflare + Firebase  
**Primary Users:** Internal development team / indie app studio  
**Product Type:** Developer Platform / Application Foundation

---

# 1. Product Overview

## 1.1 Product Vision

KMP App Starter Platform là bộ nền tảng dùng chung để xây dựng các ứng dụng Android/iOS bằng Kotlin Multiplatform.

Thay vì mỗi dự án mới phải thiết lập lại:

- Authentication
- API client
- Firebase
- Cloudflare
- Analytics
- Attribution
- Ads
- In-App Purchase
- Subscription
- Remote Config
- Feature Flags
- Push Notification
- Deep Link
- Consent
- Logging
- Crash Reporting
- Local Storage
- Database
- Debug tools

developer chỉ cần lựa chọn module cần sử dụng và bắt đầu phát triển business feature.

Mục tiêu:

> Từ ý tưởng đến một app có infrastructure production-ready trong vòng một ngày.

Starter Kit không phải là tập hợp source code copy/paste.

Nó phải hoạt động như một **Application Platform**.

---

# 2. Problem

Hiện tại khi tạo một mobile app mới, một lượng lớn thời gian không được dành cho sản phẩm mà dành cho infrastructure.

Các tác vụ thường xuyên lặp lại:

- tạo project KMP
- cấu hình Firebase
- cấu hình Cloudflare
- authentication
- networking
- token management
- analytics
- event naming
- attribution
- AdMob
- consent
- purchase
- entitlement
- push notification
- remote config
- local database
- app lifecycle
- error handling
- debug menu

Vấn đề lớn hơn việc mất thời gian là mỗi project dần hình thành một implementation khác nhau.

Hậu quả:

```text
Project A
Firebase implementation A

Project B
Firebase implementation B

Project C
Firebase implementation C
```

Sau một thời gian:

- khó maintain
- khó sửa bug đồng loạt
- analytics không đồng nhất
- attribution không đồng nhất
- monetization logic phân mảnh
- dependency SDK lan vào business logic
- migration SDK rất khó
- onboarding developer mới mất thời gian

---

# 3. Product Goals

## 3.1 Primary Goals

Starter Platform phải giúp một project mới có ngay:

### Foundation

- Kotlin Multiplatform project architecture
- dependency injection
- networking
- serialization
- local persistence
- secure storage
- logging
- error model
- environment management

### Backend

- Firebase integration
- Cloudflare API integration
- Authentication
- session management

### Growth

- analytics
- attribution
- campaign tracking
- experiment framework
- feature flags
- remote configuration

### Monetization

- ads
- purchases
- subscriptions
- entitlement
- paywall infrastructure

### Engagement

- push notifications
- local notifications
- deep links

### Developer tooling

- debug menu
- diagnostics
- sample app
- environment switching

---

# 4. Success Metrics

Starter Platform không được đánh giá bằng số lượng module.

North Star Metric:

> **Time to Product Development**

Khoảng thời gian từ lúc tạo project đến khi developer có thể bắt đầu viết business feature.

Target:

```text
Create project
↓
Configure credentials
↓
Run Android/iOS
↓
Auth working
↓
Analytics working
↓
Backend working
↓
Ready for feature development

< 1 working day
```

## Secondary metrics

### Project bootstrap

Target:

```text
< 30 phút
```

để project build thành công lần đầu.

### Shared code

Target:

```text
> 80%
```

business/infrastructure logic sử dụng chung giữa Android/iOS.

### Infrastructure reuse

Target:

```text
> 70%
```

infrastructure của các app mới đến từ Starter Platform.

### Direct SDK usage

Target:

```text
0
```

Firebase/AdMob/Revenue SDK được gọi trực tiếp từ feature layer.

---

# 5. Product Principles

## 5.1 Abstraction First

Feature không được biết implementation provider.

Không:

```kotlin
FirebaseAuth.getInstance()
```

Không:

```kotlin
FirebaseAnalytics.logEvent(...)
```

Không:

```kotlin
MobileAds.show(...)
```

Feature chỉ sử dụng:

```kotlin
AuthService

Analytics

AdService

PurchaseService

FeatureFlags
```

---

## 5.2 Replaceable Providers

Firebase, Cloudflare hoặc một SDK bên thứ ba phải có khả năng được thay thế mà không ảnh hưởng business logic.

Ví dụ:

```text
Analytics
    │
    ├ FirebaseAnalyticsAdapter
    ├ MetaAnalyticsAdapter
    ├ AppsFlyerAdapter
    └ PostHogAdapter
```

---

## 5.3 Modular

Một app không sử dụng Ads thì Ads module không được tồn tại trong runtime dependency graph.

Ví dụ:

```text
Photo App

[x] Firebase
[x] Cloudflare
[x] Analytics
[x] Ads
[x] IAP
[x] Remote Config
[ ] AI
```

---

## 5.4 Configuration Over Code Changes

Các hành vi thường xuyên thay đổi phải được cấu hình từ remote nếu phù hợp.

Ví dụ:

- ad frequency
- offer
- feature activation
- experiment
- paywall configuration

Không cần phát hành app mới cho mỗi thay đổi.

---

## 5.5 Local First Where Appropriate

UI không nên phụ thuộc trực tiếp vào network.

Preferred:

```text
UI
 ↓
Repository
 ↓
Local Source
 ↓
Remote Source
```

---

# 6. Target Users

## Primary Persona

### Product-focused mobile developer

Đặc điểm:

- xây nhiều app
- cần ship nhanh
- dùng KMP
- backend nhẹ
- không muốn vận hành server phức tạp
- sử dụng Firebase cho mobile infrastructure
- sử dụng Cloudflare cho API/business backend

Core need:

> Tôi muốn dành thời gian làm sản phẩm, không phải viết lại plumbing.

---

# 7. Product Scope

Platform gồm 7 nhóm module.

```text
starter
│
├ core
├ backend
├ identity
├ growth
├ monetization
├ engagement
└ developer-tools
```

---

# 8. Proposed Repository Structure

```text
kmp-starter/
│
├ app/
│
├ composeApp/
│
├ iosApp/
│
├ shared/
│
│
├ core/
│   ├ core-config
│   ├ core-network
│   ├ core-database
│   ├ core-storage
│   ├ core-logging
│   ├ core-platform
│   ├ core-ui
│   └ core-common
│
├ backend/
│   ├ backend-cloudflare
│   └ backend-firebase
│
├ identity/
│   ├ auth-api
│   ├ auth-firebase
│   └ session
│
├ growth/
│   ├ analytics-api
│   ├ analytics-firebase
│   ├ attribution
│   ├ experiments
│   ├ feature-flags
│   └ remote-config
│
├ monetization/
│   ├ ads-api
│   ├ ads-admob
│   ├ purchase-api
│   ├ purchase-store
│   ├ entitlement
│   └ paywall
│
├ engagement/
│   ├ notification
│   ├ deep-link
│   └ lifecycle
│
├ devtools/
│   ├ debug-menu
│   └ diagnostics
│
└ sample/
```

---

# 9. Core Module

## 9.1 Core Config

Responsible for:

- app information
- build information
- environment
- service configuration
- capability flags

Example:

```kotlin
data class AppConfig(
    val appId: String,
    val environment: Environment,
    val backend: BackendConfig,
    val capabilities: Capabilities
)
```

---

# 10. Environment Management

Supported environments:

```text
DEV

STAGING

PRODUCTION
```

Environment controls:

- Cloudflare API URL
- Firebase project
- logging
- analytics
- Ad Unit IDs
- remote config
- debug capabilities

Không sử dụng:

```text
if (debug)
```

cho logic environment.

---

# 11. Networking

Networking module cung cấp:

```text
ApiClient

Request interceptor

Authentication header

Retry policy

Timeout

Serialization

Error mapping
```

Business code gọi:

```kotlin
repository.getProfile()
```

không xử lý:

```text
HTTP 401

HTTP 429

HTTP 500
```

trực tiếp.

---

# 12. Unified Error Model

Tất cả lỗi được map về:

```kotlin
sealed interface AppError
```

Recommended categories:

```text
NetworkError

AuthError

ValidationError

RateLimitError

ServerError

PermissionError

StorageError

UnknownError
```

Cloudflare API có standardized error code:

```text
AUTH_REQUIRED

INVALID_REQUEST

RATE_LIMITED

NOT_FOUND

FEATURE_DISABLED

SERVER_ERROR
```

---

# 13. API Contract

Default API response:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "meta": {}
}
```

Error:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "RATE_LIMITED",
    "message": "Too many requests"
  }
}
```

---

# 14. Cloudflare Backend

Cloudflare được sử dụng cho:

- REST API
- business logic
- AI proxy
- caching
- rate limiting
- signed requests
- webhooks
- cron jobs

Supported Cloudflare products:

```text
Workers

D1

KV

R2

Queues

Cron Triggers
```

---

# 15. Cloudflare Backend Template

Structure:

```text
backend/
│
├ src/
│   ├ routes/
│   ├ middleware/
│   ├ domain/
│   ├ services/
│   ├ repositories/
│   └ utils/
│
├ migrations/
│
├ tests/
│
└ wrangler.toml
```

Initial API:

```text
GET /health

GET /config

GET /user

POST /event

POST /auth/session

GET /feature-flags

POST /webhook/*
```

---

# 16. Backend Authentication

KMP client sends Firebase ID token.

```text
KMP App
   ↓
Firebase Auth
   ↓
ID Token
   ↓
Cloudflare Worker
   ↓
Verify token
   ↓
Request context
```

Backend RequestContext:

```text
userId

anonymousId

appVersion

platform

country

language
```

---

# 17. Authentication

Interface:

```kotlin
interface AuthService {

    val currentUser: Flow<User?>

    suspend fun login(
        provider: AuthProvider
    ): Result<User>

    suspend fun logout()

}
```

Supported providers:

```text
Anonymous

Google

Apple

Email

Magic Link
```

---

# 18. Anonymous First

Default recommended flow:

```text
Install
 ↓
Anonymous account
 ↓
Use product
 ↓
Create content
 ↓
Optional authentication
 ↓
Account linking
```

Rationale:

Không bắt người dùng tạo account trước khi nhận được giá trị từ sản phẩm.

---

# 19. Session Manager

Responsibilities:

```text
current user

anonymous identity

access token

token refresh

logout

account linking

session lifecycle
```

Feature không quản lý token.

---

# 20. User Identity

Platform phải duy trì identity thống nhất.

```text
anonymousId

firebaseUid

installId

attributionId
```

Unified:

```kotlin
UserIdentity
```

Analytics, backend và monetization sử dụng cùng identity context.

---

# 21. Analytics

Feature chỉ gọi analytics abstraction:

```kotlin
analytics.track(
    PhotoSaved(...)
)
```

Không gọi provider SDK trực tiếp.

---

# 22. Typed Analytics Events

Không sử dụng event string tùy ý.

Preferred:

```kotlin
sealed interface AnalyticsEvent
```

Core events:

```text
AppOpened

SessionStarted

ScreenViewed

OnboardingStarted

OnboardingCompleted

FeatureUsed

AdViewed

PaywallViewed

PurchaseStarted

PurchaseCompleted

PurchaseFailed
```

---

# 23. Analytics Destinations

Một event có thể gửi đến:

```text
Firebase Analytics

Meta

AppsFlyer

PostHog

Other providers
```

Router:

```text
AnalyticsEvent
       │
       ↓
AnalyticsRouter
   │   │   │
   ↓   ↓   ↓
 FB   Meta AppsFlyer
```

---

# 24. Event Naming Convention

Recommended:

```text
noun_action
```

Ví dụ:

```text
photo_saved

filter_applied

paywall_viewed

purchase_started

purchase_completed
```

Không cho phép tự tạo arbitrary naming trong feature.

---

# 25. User Properties

Global analytics properties:

```text
user_type

premium_status

country

language

app_version

platform

install_age

session_count
```

Domain app có thể bổ sung properties riêng.

---

# 26. Attribution

Attribution module chịu trách nhiệm:

```text
install source

campaign

adset

creative

deep link

referrer
```

Normalized structure:

```kotlin
AttributionInfo(
    source,
    campaign,
    adGroup,
    creative
)
```

---

# 27. Feature Flags

Interface:

```kotlin
interface FeatureFlags {

    fun isEnabled(
        key: FeatureKey
    ): Boolean

}
```

Example:

```text
NEW_EDITOR

AI_FEATURE

INSPIRATION_FEED

NEW_PAYWALL
```

---

# 28. Remote Config

Không giới hạn ở boolean.

Config có thể chứa:

```text
number

string

JSON object
```

Example:

```json
{
  "lifetime_offer": {
    "enabled": true,
    "price": 29.99,
    "min_app_opens": 5
  }
}
```

Provider:

```text
Firebase Remote Config

Cloudflare KV
```

---

# 29. Experiment Framework

API:

```kotlin
experiment.variant(
    Experiment.PAYWALL_V2
)
```

Result:

```text
CONTROL

A

B
```

Mỗi analytics event tự attach:

```text
experiment_id

variant
```

---

# 30. Experiment Assignment

Variant phải:

- deterministic
- stable cho một user
- không đổi giữa sessions
- có thể override trong Debug Menu

Recommended hash:

```text
userId + experimentId
```

---

# 31. Domain Events

Starter Kit có lightweight domain event system.

Ví dụ:

```text
PhotoSaved

UserLoggedIn

PurchaseCompleted

AdWatched

OnboardingCompleted
```

Flow:

```text
PurchaseCompleted
       │
 ┌─────┼──────────┐
 ↓     ↓          ↓
Analytics User   OfferState
```

Không sử dụng global event bus cho UI communication.

Domain events dành cho cross-cutting infrastructure.

---

# 32. Ads

Public interface:

```kotlin
interface AdService {

    suspend fun preload(
        placement: AdPlacement
    )

    suspend fun show(
        placement: AdPlacement
    ): AdResult
}
```

---

# 33. Ad Placement

Feature sử dụng semantic placement.

Ví dụ:

```text
APP_OPEN

HOME_BANNER

SAVE_PHOTO_INTERSTITIAL

EXPORT_REWARDED
```

Không truyền trực tiếp Ad Unit ID.

---

# 34. Remote Ad Configuration

Example:

```json
{
  "SAVE_PHOTO_INTERSTITIAL": {
    "enabled": true,
    "frequency": 3
  }
}
```

Server có thể kiểm soát:

```text
enabled

frequency

cooldown

user segment

country

premium exclusion
```

---

# 35. Ad Rules Engine

Example:

```text
Free User
+
3 eligible actions
+
not shown within cooldown
+
consent granted
=
Show Ad
```

Rules không nằm trong UI.

---

# 36. Monetization

Monetization module gồm:

```text
Product

Purchase

Subscription

Entitlement

Offer

Paywall
```

---

# 37. Entitlement-first Design

Business logic không check product ID.

Không:

```kotlin
isPurchased(
    "premium_yearly_2026"
)
```

Preferred:

```kotlin
entitlements.has(
    Entitlement.PREMIUM
)
```

---

# 38. Default Entitlements

Platform built-in:

```text
FREE

PREMIUM

LIFETIME
```

Apps có thể mở rộng:

```text
STARTER_PACK

PRO_FILTERS

AI_CREDITS
```

---

# 39. Product Model

```kotlin
Product(
    id,
    type,
    entitlement,
    price,
    currency
)
```

Types:

```text
CONSUMABLE

NON_CONSUMABLE

SUBSCRIPTION
```

---

# 40. Purchase State

```text
Idle

Loading

Success

Cancelled

Pending

Failed
```

Purchase state phải reusable trên Android/iOS.

---

# 41. Paywall

Starter Kit không nên chứa một paywall cố định.

Nó cung cấp:

```text
PaywallModel

Product presentation

Purchase CTA

Restore purchase

Legal links

Loading states

Error states
```

UI có thể tùy biến theo app.

---

# 42. Notifications

Notification module bao gồm:

```text
Push notifications

Local notifications

Token management

Permission state

Notification routing
```

Push provider mặc định:

```text
Firebase Cloud Messaging
```

---

# 43. Push Payload

Standard:

```json
{
  "type": "PROMOTION",
  "route": "premium_offer",
  "data": {}
}
```

Notification layer chuyển route sang DeepLinkRouter.

---

# 44. Deep Links

Unified routes:

```text
premium

profile

settings

feature/{id}
```

Example external URI:

```text
myapp://premium
```

Internal app sử dụng typed route.

---

# 45. Permission Manager

Supported permission types:

```text
Camera

Photos

Microphone

Notification

Location

Tracking
```

Interface:

```kotlin
PermissionManager
```

States:

```text
NotDetermined

Granted

Denied

Restricted
```

---

# 46. Consent

Consent là module riêng, không gộp với Ads.

Supported concerns:

```text
GDPR

Analytics Consent

Ads Consent

Ads Personalization

ATT
```

Analytics/Ads providers chỉ được start khi policy tương ứng cho phép.

---

# 47. App Lifecycle

Unified events:

```text
AppLaunch

Foreground

Background

SessionStart

SessionEnd
```

Consumers:

```text
Analytics

Ads

Remote Config

Sync

Notification
```

---

# 48. Persistence

Starter hỗ trợ hai mức lưu trữ.

## Preferences

Dùng cho:

```text
settings

feature states

small configuration
```

## Database

Dùng cho:

```text
structured data

offline cache

domain objects
```

Recommended abstraction:

```text
Database

KeyValueStorage

SecureStorage
```

---

# 49. Cache Policy

Repositories có thể khai báo:

```text
CACHE_FIRST

NETWORK_FIRST

CACHE_ONLY

NETWORK_ONLY

STALE_WHILE_REVALIDATE
```

---

# 50. Offline Behavior

Default rule:

Không hiển thị generic error ngay lập tức nếu cached data tồn tại.

Expected flow:

```text
Open screen
 ↓
Show cached data
 ↓
Refresh network
 ↓
Update cache
 ↓
Update UI
```

---

# 51. Standard UI State

Starter cung cấp:

```kotlin
sealed interface UiState<out T> {

    data object Loading

    data class Content<T>(
        val data: T
    )

    data object Empty

    data class Error(
        val error: AppError
    )
}
```

Screens phải xử lý:

```text
Loading

Content

Empty

Offline

Error
```

khi applicable.

---

# 52. Design System

Starter chỉ cung cấp primitive reusable.

Không áp branding cụ thể.

Components:

```text
AppButton

AppTextField

AppDialog

AppBottomSheet

AppToast

LoadingState

EmptyState

ErrorState

PermissionDialog
```

Theme:

```text
Color

Typography

Spacing

Shape

Elevation
```

---

# 53. Logging

Unified logger:

```kotlin
Logger.debug()

Logger.info()

Logger.warn()

Logger.error()
```

Không sử dụng `println()` trong production modules.

---

# 54. Crash Reporting

Default provider:

```text
Firebase Crashlytics
```

Crash context có thể attach:

```text
userId

app version

screen

feature flags

experiment variants
```

Không attach sensitive data.

---

# 55. Diagnostics

Diagnostics screen hiển thị:

```text
App name

Version

Build

Platform

OS version

Environment

Firebase UID

Anonymous ID

Subscription state

API endpoint

Remote Config version

Experiment assignments
```

Có action:

```text
Copy Diagnostics
```

---

# 56. Debug Menu

Chỉ xuất hiện trong developer builds.

Capabilities:

```text
Environment override

User information

Premium override

Feature Flag override

Experiment override

Remote Config refresh

Trigger test ad

Trigger notification

Clear cache

Reset onboarding

Logout

API diagnostics
```

---

# 57. Sample App

Starter Platform phải có một showcase app.

Home:

```text
KMP Starter

Auth
Connected

Cloudflare
Connected

Analytics
Active

Ads
Ready

Purchases
Ready

Remote Config
Loaded

Push
Ready
```

Developer có thể test từng integration.

---

# 58. Sample Test Actions

```text
Login

Logout

Call API

Track event

Fetch Remote Config

Change feature flag

Show rewarded ad

Show interstitial

Load products

Test purchase

Test deep link

Schedule notification

Create test crash
```

---

# 59. Starter CLI — Future Layer

Command:

```bash
kmpstarter create
```

Wizard:

```text
Project name?

Package name?

Firebase?

Cloudflare?

Authentication providers?

Analytics?

Ads?

Purchases?

Push?

Database?
```

Output:

```text
configured KMP application
```

---

# 60. Project Manifest

Potential configuration:

```yaml
app:
  name: Retro Camera
  package: com.company.retro

platforms:
  android: true
  ios: true

backend:
  firebase: true
  cloudflare: true

auth:
  anonymous: true
  google: true
  apple: true

growth:
  analytics: true
  attribution: true
  remoteConfig: true
  experiments: true

monetization:
  ads: true
  purchases: true

engagement:
  push: true
  deepLinks: true
```

---

# 61. Bootstrap Workflow

Target future workflow:

```text
kmpstarter create
       ↓
Choose modules
       ↓
Generate project
       ↓
Configure secrets
       ↓
Run
```

No manual deletion of unused modules.

---

# 62. Configuration Strategy

Configuration được chia thành:

### Compile-time config

```text
package

bundle ID

capabilities

environment
```

### Secret config

```text
API secrets

provider credentials
```

### Remote config

```text
feature behavior

offers

ads

experiments
```

---

# 63. Secrets

Secrets không được commit vào repository.

Support:

```text
local.properties

environment variables

CI secrets

Cloudflare secrets
```

---

# 64. CI/CD

Starter nên cung cấp CI templates cho:

```text
lint

unit test

Android build

iOS build

backend tests
```

Future:

```text
Play Store deployment

TestFlight deployment
```

---

# 65. Testing Strategy

## Unit Tests

Focus:

```text
repositories

business logic

experiment assignment

entitlement

ads rules

auth state
```

## Integration Tests

Focus:

```text
Cloudflare API

Firebase Auth

Remote Config

Purchases
```

## Smoke Tests

Sample App kiểm tra:

```text
startup

login

API

analytics

config
```

---

# 66. Observability

Infrastructure events:

```text
network_error

auth_error

config_load_failed

purchase_error

ad_load_failed
```

Phân biệt:

```text
Product Analytics

vs

System Observability
```

Không trộn hai loại vào cùng dashboard/event taxonomy.

---

# 67. Security

Minimum requirements:

- TLS
- verify Firebase authentication server-side
- no API secret embedded in client
- secure local credential storage
- rate limiting
- request validation
- backend authorization
- least-privilege configuration

Client không được coi là trusted environment.

---

# 68. Privacy

Starter phải có utility hỗ trợ:

```text
delete account

clear local data

reset analytics identity

consent withdrawal
```

Logging không được chứa:

```text
password

tokens

personal content
```

---

# 69. Performance Requirements

Starter Kit infrastructure không được khiến startup phụ thuộc vào hàng loạt SDK.

Initialization phải chia:

```text
Critical

Deferred

On-demand
```

Critical:

```text
Config

Storage

Auth session
```

Deferred:

```text
Analytics

Crash

Attribution
```

On-demand:

```text
Ads

Purchases
```

khi phù hợp với app.

---

# 70. Initialization Coordinator

Thay vì mỗi SDK tự start:

```text
App
 ↓
InitializationCoordinator
 ↓
Critical tasks
 ↓
App usable
 ↓
Deferred tasks
```

Starter cần tránh startup waterfall.

---

# 71. Dependency Rules

Desired dependency direction:

```text
Feature
   ↓
Domain API
   ↓
Infrastructure abstraction
   ↓
Provider implementation
```

Provider không được import ngược feature.

---

# 72. Non-Goals

V1 Starter Platform không cung cấp:

```text
Social Feed

Chat

Photo Editor

AI feature

Gamification

User Profile UX

CMS

Admin Dashboard
```

Các feature này thuộc product-specific layer.

---

# 73. Scope Rule

Một module chỉ nên được đưa vào Starter Platform nếu:

> Có khả năng được sử dụng trong ít nhất ~70% app của studio.

Nếu thấp hơn, module nên tồn tại dạng optional package hoặc product-specific library.

---

# 74. Risks

## Risk 1 — Over-engineering

Starter Kit rất dễ biến thành một framework lớn trước khi có nhu cầu thực tế.

Mitigation:

Chỉ extract abstraction sau khi có use case thực.

---

## Risk 2 — Abstraction quá sâu

Wrapper mọi SDK API sẽ tạo thêm complexity.

Mitigation:

Chỉ abstract những phần business layer thực sự phụ thuộc.

---

## Risk 3 — Mega Template

Một repository chứa mọi tính năng khiến:

```text
clone
↓
delete 40% code
```

Đây là dấu hiệu kiến trúc sai.

Solution:

Modular packages + optional capability system.

---

## Risk 4 — Shared Code Become Product Logic

Logic riêng của một app có thể bị đưa vào starter vì developer muốn reuse.

Rule:

Starter chứa infrastructure.

Product chứa experience.

---

# 75. MVP Scope

V1 không cần CLI.

MVP chỉ cần:

### Core

```text
Config

Environment

Networking

Serialization

Storage

Logging

Error Model

DI
```

### Backend

```text
Cloudflare

Firebase
```

### Identity

```text
Anonymous Auth

Google Auth

Apple Auth

Session
```

### Growth

```text
Analytics

Remote Config

Feature Flags
```

### Monetization

```text
Ads

Purchases

Entitlements
```

### Engagement

```text
Push

Deep Links
```

### Tools

```text
Debug Menu

Diagnostics

Sample App
```

---

# 76. Post-MVP

V1.1:

```text
Attribution

Experiments

Consent framework

Offline synchronization
```

V1.2:

```text
Starter CLI

Project generator

CI templates
```

V2:

```text
Internal package registry

Module marketplace

Backend provisioning

Project dashboard
```

---

# 77. Development Milestones

## Phase 1 — Foundation

Deliver:

```text
Project architecture

Environment

Network

Storage

Logging

Error handling
```

Exit condition:

Sample app runs successfully on Android/iOS.

---

## Phase 2 — Identity & Backend

Deliver:

```text
Firebase

Cloudflare

Anonymous authentication

Google

Apple

Session management
```

Exit condition:

Authenticated Cloudflare API request works from Android/iOS.

---

## Phase 3 — Growth Infrastructure

Deliver:

```text
Analytics

User identity

Feature flags

Remote config
```

Exit condition:

One event can be emitted from shared business code and received by configured analytics provider.

---

## Phase 4 — Monetization

Deliver:

```text
Ads abstraction

AdMob implementation

Purchase abstraction

Entitlement
```

Exit condition:

Sample app can:

```text
show rewarded ad

purchase product

unlock entitlement
```

---

## Phase 5 — Engagement & Operations

Deliver:

```text
Push

Deep link

Debug menu

Diagnostics
```

---

## Phase 6 — Developer Experience

Deliver:

```text
Documentation

Example implementations

Project setup script

CLI generator
```

---

# 78. MVP Acceptance Criteria

A fresh project created from Starter Kit must support:

- Android build
- iOS build
- DEV/STAGING/PROD environments
- authenticated API calls
- Firebase authentication
- Cloudflare backend
- standardized errors
- analytics tracking
- remote config
- feature flags
- ads
- purchases
- entitlement
- push notification
- deep links
- debug menu
- diagnostics

Developer must not need to modify internal starter infrastructure to create a basic app.

---

# 79. Developer Experience Requirement

A new app should conceptually require only:

```text
1. Create project
2. Configure IDs/secrets
3. Select modules
4. Implement product features
```

Không nên yêu cầu developer hiểu toàn bộ internal implementation của Starter Platform.

---

# 80. Documentation Requirements

Mỗi module phải có:

```text
Purpose

Setup

Public API

Example

Configuration

Common errors

Testing instructions
```

Ngoài ra cần:

```text
Getting Started

Architecture

Create New Project

Add New Module

Add New Analytics Event

Add New Backend Route

Release Checklist
```

---

# 81. Recommended API Philosophy

Public APIs phải:

```text
small

typed

stable

discoverable
```

Ví dụ tốt:

```kotlin
analytics.track(
    PaywallViewed(source)
)
```

Không tốt:

```kotlin
analytics.send(
    name = "...",
    params = mapOf(...)
)
```

Typed API giúp tránh analytics entropy khi số lượng app tăng.

---

# 82. Strategic Challenge

Sai lầm dễ xảy ra nhất là biến Starter Kit thành:

> Một project mẫu thật lớn có tất cả mọi thứ.

Giá trị thực sự của hệ thống này không nằm ở số module.

Giá trị nằm ở việc:

```text
App A
App B
App C
App D
```

đều sử dụng cùng một cách cho:

```text
Identity

Analytics

Backend

Ads

Purchases

Experiments
```

Từ đó một improvement trong platform có thể tác động đến toàn bộ portfolio app.

---

# 83. Long-term Architecture

Mục tiêu cuối cùng:

```text
                 KMP APP PLATFORM
                        │
       ┌────────────────┼────────────────┐
       │                │                │
    Photo App       Habit App       Wardrobe App
       │                │                │
       └────────────────┼────────────────┘
                        │
                Shared Infrastructure
                        │
           ┌────────────┴────────────┐
           │                         │
       Firebase                 Cloudflare
```

Ở thời điểm đó, studio không còn tạo từng app từ đầu.

Studio đang vận hành:

> **một mobile application platform tạo ra nhiều sản phẩm.**

---

# 84. Final Product Definition

KMP Starter Platform không được định nghĩa là:

> “Template KMP có Firebase + Cloudflare.”

Nó nên được định nghĩa là:

> **Reusable mobile application infrastructure cho phép xây dựng, đo lường, monetize và vận hành nhiều ứng dụng Android/iOS trên cùng một architecture.**

V1 cần ưu tiên đúng thứ tự:

```text
Infrastructure consistency
        ↓
Developer speed
        ↓
Observability
        ↓
Growth infrastructure
        ↓
Monetization
```

Không ưu tiên số lượng feature.

Mỗi module chỉ xứng đáng tồn tại nếu nó làm một trong ba việc:

1. giảm code lặp lại đáng kể,
2. tạo chuẩn chung giữa nhiều app,
3. cho phép thay đổi hệ thống mà không phá business layer.