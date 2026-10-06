---
name: optimize-store-metadata
description: >-
  App Store Optimization (ASO) guide for App Store and Google Play metadata — optimize Title (30 chars),
  Subtitle (30 chars), iOS Keywords field (100 chars, comma-separated with no spaces), and Google Play
  Short Description (80 chars) to maximize organic search visibility and conversion. Use during Phase 3
  `publishing` and Phase 5 `growth`.
---

# Store Metadata Optimization (ASO Playbook)

This skill guides you through crafting high-ranking, high-converting metadata for **Apple App Store**
and **Google Play Store**. High-quality metadata directly improves organic search visibility (App Store
Optimization - ASO) and click-through rates.

Use this when filling metadata during `setup-appstore-connect` and `setup-google-play`.

---

## 1. Apple App Store Optimization (iOS)

Apple's search algorithm indexes three fields: **App Name**, **Subtitle**, and the hidden **Keywords** field.

### A. App Name (Max 30 characters)
- **Formula**: `[Brand Name]: [Core Keyword / Primary Job]`
- **Examples**:
  - `Koko: Habit Tracker & Routine` (30 chars)
  - `PulseFit: Workout Planner` (26 chars)
  - `CalorieAI: Food Macro Tracker` (30 chars)
- **Rules**: Do not duplicate words across Title and Subtitle. Avoid terms like "Best", "#1", or "Free" (Apple will reject).

### B. Subtitle (Max 30 characters)
- **Formula**: `[Secondary Benefit / Action Hook]`
- **Examples**:
  - `Build Daily Streaks & Goals` (28 chars)
  - `Track Meals with Photo AI` (26 chars)
  - `Simple Weight Loss & Health` (28 chars)

### C. Keywords Field (Max 100 characters)
The 100-character keyword field is invisible to users but critical for search rankings.
- **Strict Apple Keyword Syntax**:
  - Separate words with commas with **NO spaces** after commas (`habit,tracker,routine,planner`).
  - **Never repeat** words already in App Name or Subtitle (Apple indexes them automatically; repeating wastes precious characters).
  - Use singular nouns (Apple matches plurals automatically).
  - Use digits instead of words (`7` instead of `seven`).
  - Do NOT include category names (e.g. `health`, `productivity` if your app is already in that category).
  - Do NOT include trademarked competitor names (causes rejection).
- **Example 100-char string**:
  `streak,reminder,checklist,diary,coach,motivation,bullet,journal,task,schedule,pomodoro,organizer` (96 chars)

---

## 2. Google Play Store Optimization (Android)

Google's search algorithm crawls the **Title**, **Short Description**, and **Full Description**.

### A. App Title (Max 30 characters)
- Similar to iOS: Clear brand name + primary search term.

### B. Short Description (Max 80 characters)
- The single most critical conversion text on Google Play (displayed above the fold).
- **Formula**: A compelling hook stating the core transformation.
- **Examples**:
  - *"Build lasting daily habits, stay motivated, and achieve your goals with ease."* (79 chars)
  - *"Track your workouts, log sets, and reach your fitness targets with smart plans."* (78 chars)

### C. Long Description (Max 4,000 characters)
- Write for humans first, search engines second:
  - **First 3 lines**: The core hook and value proposition.
  - **Bullet points**: Clear feature breakdowns with natural keyword inclusion (target 2-3% keyword density).
  - Include social proof, awards, or community size.

---

## 3. Metadata Verification Template

Fill out this worksheet before pasting into App Store Connect and Google Play Console:

```markdown
### iOS App Store Metadata
- [ ] App Name (≤ 30): ____________________ (Len: __)
- [ ] Subtitle (≤ 30): ____________________ (Len: __)
- [ ] Keywords (≤ 100, no spaces): ____________________ (Len: __)
- [ ] No duplicate words between Name, Subtitle, and Keywords
- [ ] No prohibited terms ("free", "#1", "top", trademarked brands)

### Google Play Store Metadata
- [ ] App Name (≤ 30): ____________________ (Len: __)
- [ ] Short Description (≤ 80): ____________________ (Len: __)
- [ ] Privacy Policy URL accessible and HTTPS: ____________________
```
