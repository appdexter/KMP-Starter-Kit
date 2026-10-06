---
name: admob-roas
description: >-
  Implement AdMob ROAS tracking and Impression-Level Ad Revenue (ILR) in KMPStarterKit — wire
  OnPaidEventListener on Android and paidEventHandler on iOS to emit AnalyticsEvent.AdImpression to
  Firebase Analytics for Google Ads target ROAS (tROAS) bidding. Use when setting up ROAS tracking,
  measuring ad revenue LTV, or optimizing Google App Campaigns. Part of the `monetization` phase.
---

# AdMob ROAS & Impression-Level Revenue Tracking

This skill guides you through capturing **Impression-Level Ad Revenue (ILR)** from Google AdMob and
forwarding it to Firebase Analytics and MMPs as an `ad_impression` event. This is required for
**Google Ads Target ROAS (tROAS)** bidding campaigns and accurate Lifetime Value (LTV) calculation.

The data layer already has typed support via `AnalyticsEvent.AdImpression` in
`shared/src/commonMain/kotlin/com/kotlinfoundation/koko/growth/analytics/AnalyticsEvent.kt`.

---

## 1. Why AdMob ROAS Matters

Without impression revenue tracking, Google Ads and Firebase only see *that* an ad showed, but not
*how much revenue* it generated. With `OnPaidEventListener`:
1. Every impression reports exact micropayments (e.g. `$0.0042` from an interstitial).
2. Firebase logs the standard `ad_impression` event with `value` and `currency`.
3. Google Ads Universal App Campaigns (UAC) automatically optimize bids toward users who generate higher ad revenue.

---

## 2. Android Implementation (`androidMain`)

In Android AdMob SDK, every ad format (`AdView`, `InterstitialAd`, `RewardedAd`) provides an
`setOnPaidEventListener`.

### A. Banner Ad (`AdView`)
When creating the banner ad view in Android platform code:

```kotlin
adView.setOnPaidEventListener { adValue ->
    val valueMicros = adValue.valueMicros
    val currencyCode = adValue.currencyCode
    val precisionType = adValue.precisionType

    analyticsTracker.logEvent(
        AnalyticsEvent.AdImpression(
            adPlacement = "banner",
            adNetwork = "admob",
            valueMicros = valueMicros,
            currency = currencyCode,
            adUnitId = adView.adUnitId,
            adFormat = "BANNER"
        )
    )
}
```

### B. Interstitial & Rewarded Ads
Attach the listener immediately after the ad is loaded:

```kotlin
InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
    override fun onAdLoaded(interstitialAd: InterstitialAd) {
        interstitialAd.setOnPaidEventListener { adValue ->
            analyticsTracker.logEvent(
                AnalyticsEvent.AdImpression(
                    adPlacement = "interstitial",
                    adNetwork = "admob",
                    valueMicros = adValue.valueMicros,
                    currency = adValue.currencyCode,
                    adUnitId = interstitialAd.adUnitId,
                    adFormat = "INTERSTITIAL"
                )
            )
        }
    }
})
```

---

## 3. iOS Implementation (`iosMain`)

On iOS (Google-Mobile-Ads SDK), assign `paidEventHandler` on the ad objects:

```swift
interstitialAd.paidEventHandler = { [weak self] adValue in
    let value = adValue.value.doubleValue
    let currency = adValue.currencyCode
    let precision = adValue.precision

    // Emit ad_impression to shared AnalyticsTracker
    AnalyticsBridge.shared.logAdImpression(
        placement: "interstitial",
        valueMicros: Int64(value * 1_000_000),
        currency: currency,
        adUnitId: interstitialAd.adUnitID,
        adFormat: "INTERSTITIAL"
    )
}
```

---

## 4. Firebase Event Payload Verification

The Firebase Analytics receiver maps `AnalyticsEvent.AdImpression` to Firebase's native
`FirebaseAnalytics.Event.AD_IMPRESSION`:

| Parameter | Type | Example | Purpose |
|---|---|---|---|
| `ad_platform` | String | `"admob"` | Identifies ad mediation provider |
| `ad_source` | String | `"AdMob"` | Specific ad network filling impression |
| `ad_unit_name` | String | `"ca-app-pub-xxx/yyy"` | Placement identifier |
| `ad_format` | String | `"INTERSTITIAL"` | Format type (BANNER, INTERSTITIAL, REWARDED) |
| `value` | Double | `0.0125` | Revenue in currency (derived from `valueMicros / 1,000,000.0`) |
| `currency` | String | `"USD"` | ISO 4217 Currency code |

### Verification via Firebase DebugView
1. Enable DebugView on Android:
   ```bash
   adb shell setprop debug.firebase.analytics.app com.kotlinfoundation.koko
   ```
2. Trigger an ad impression in the app.
3. Open **Firebase Console ➔ Analytics ➔ DebugView**.
4. Confirm `ad_impression` appears with green `value` and `currency` attributes.

---

## 5. Google Ads Linking Checklist

Once impressions log to Firebase:
1. In Firebase Console, go to **Project Settings ➔ Integrations ➔ Google Ads** and link your Google Ads account.
2. Under **Analytics ➔ Conversions**, mark `ad_impression` as a conversion.
3. In Google Ads, import the `ad_impression` conversion event.
4. You can now select **Target ROAS** bidding in Google App Campaigns!
