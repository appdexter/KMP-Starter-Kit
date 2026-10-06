---
name: paywall-upgrade-cro
description: >-
  Conversion Rate Optimization (CRO) guide for the KMPStarterKit paywall — price anchoring,
  annual vs monthly framing, free trial risk reversal ("cancel anytime"), goal-oriented benefits, and
  trust badges to maximize trial starts and paid subscriptions. Use after `design-paywall` or when
  optimizing paywall conversion rate. Part of the `monetization` phase.
---

# Paywall CRO (Conversion Rate Optimization)

This skill provides an empirical, data-driven optimization playbook to turn the baseline
`PaywallScreen` in KMPStarterKit into a high-converting revenue driver.

The starter kit's paywall UI lives at:
`shared/src/commonMain/kotlin/com/kotlinfoundation/koko/presentation/screens/paywall/`
and its copy resides in `composeResources/values/strings.xml`.

---

## 1. The 5 Pillars of High-Converting Mobile Paywalls

```
┌─────────────────────────────────────────────────────────┐
│ [1. GOAL-ORIENTED HOOK]                                 │
│ "Unlock Unlimited Habits & AI Analysis"                 │
│                                                         │
│ [2. 3 CONCRETE BENEFITS]                                │
│  ✔ Unlimited streak tracking (no limits)                │
│  ✔ Daily smart coaching and voice notes                 │
│  ✔ Cloud backup and cross-device sync                   │
│                                                         │
│ [3. PRICE ANCHORING]                                    │
│  ┌───────────────────────┐   ┌───────────────────────┐  │
│  │ Monthly               │   │ ANNUAL (BEST VALUE)   │  │
│  │ $9.99 / month         │   │ $3.33 / mo (Save 67%) │  │
│  │                       │   │ 7-day Free Trial      │  │
│  └───────────────────────┘   └───────────────────────┘  │
│                                                         │
│ [4. RISK-REVERSAL REASSURANCE]                          │
│ "No commitment • Cancel anytime in Settings"            │
│ "We'll send a reminder 2 days before trial ends"        │
│                                                         │
│ [5. ONE-TAP HIGH-CONTRAST CTA]                          │
│ [ Start My 7-Day Free Trial ]                           │
└─────────────────────────────────────────────────────────┘
```

---

## 2. CRO Strategy Checklist

### A. Price Anchoring & Package Framing
- **Default Selection**: Pre-select the **Annual** plan by default.
- **Per-Month Breakdown**: Always display the annual price broken down to its monthly equivalent (e.g. `"$39.99/yr ($3.33/mo)"`). Users compare `$3.33/mo` against the standalone `$9.99/mo`.
- **Badge Contrast**: The Annual package should feature a high-contrast `"BEST VALUE"` or `"SAVE 65%"` badge.
- Edit `paywall_sub_package_badge_best_value` and `paywall_sub_package_annual_savings` in `strings.xml`.

### B. Risk-Reversal & Trial Anxiety Reducers
Fear of unexpected charges is the #1 reason users abandon paywalls:
- Include the reminder reassurance copy right above the CTA button:
  - *"7 days free, then $39.99/year. Cancel anytime with 1 tap in Store settings."*
  - *"We will remind you 2 days before your trial ends."*
- Set `paywall_sub_reassurance` in `strings.xml`.

### C. Goal-Oriented Feature Bullets (Max 3)
Do **not** list technical features. List the transformation the user achieves:
- ❌ *"Room database sync"* ➔ ✅ *"Never lose your data with instant cloud sync"*
- ❌ *"OpenAI API integration"* ➔ ✅ *"Personalized daily AI recommendations"*
- ❌ *"Advanced export formats"* ➔ ✅ *"Export clean PDF reports for your doctor/team"*

### D. Legal & Apple Review Compliance
Never sacrifice compliance for conversion — Apple will reject your submission:
- Keep the **Restore Purchases** text button visible in the top toolbar or footer.
- Keep **Terms of Service** and **Privacy Policy** clickable links visible without scrolling.

---

## 3. Applying CRO Tweaks to Strings & State

All copy changes flow into `shared/src/commonMain/composeResources/values/strings.xml`:

```xml
<!-- CRO Hook & Transformation -->
<string name="paywall_sub_title">Reach your goals 3x faster</string>
<string name="paywall_sub_subtitle">Join 10,000+ users building lasting habits every day.</string>

<!-- Risk Reversal CTA -->
<string name="paywall_sub_cta_trial">Start My 7-Day Free Trial</string>
<string name="paywall_sub_reassurance">Cancel anytime. No charges if cancelled during trial.</string>

<!-- Badges -->
<string name="paywall_badge_best_value">MOST POPULAR</string>
<string name="paywall_sub_package_annual_savings">SAVE 67%</string>
```

---

## 4. Visual Verification

Verify the visual hierarchy and readability on light and dark themes using `verify-ui`:

```bash
cd MobileApp
# Record fresh Roborazzi screenshot of the Paywall preview
./gradlew :shared:recordRoborazziAndroidHostTest --tests "*PaywallPreviewScreenshotTest*"
```

Inspect the generated snapshot in `shared/src/androidHostTest/snapshots/` to ensure:
1. CTA is above the fold or sticks cleanly to the bottom.
2. Badge text is legible without wrapping awkwardly.
3. Insets do not clip the "Restore Purchases" button on notches or Dynamic Island.
