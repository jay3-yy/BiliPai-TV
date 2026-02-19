# UX Priority Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Prioritize user-facing UX improvements across top bar, Today Watch plugin, and portrait playback to maximize perceived quality and retention with controlled rollout risk.

**Architecture:** Deliver in three waves: bug/usability fixes first, visual polish second, personalization capability third. Each wave is split into independently shippable tasks behind settings flags where possible for easy rollback.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation Compose, SettingsManager/DataStore, existing Home/Video policy modules.

---

### Priority Model

- `P0` = high user pain + low/medium implementation risk + immediate release value
- `P1` = medium user pain or higher dependency cost, scheduled after P0 stability
- `P2` = capability expansion, staged after core UX stabilizes

### P0 (Ship First)

#### 1) Portrait mode functional fixes

**Scope**
- Resolve duplicated/low-value actions in portrait controls.
- Fix share payload missing video title.
- Move rotate-to-landscape entry to right-bottom cluster (or equivalent thumb zone).
- Add long-press speed-up in portrait flow if current screen path lacks it.

**Primary files**
- `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/PortraitFullscreenOverlay.kt`
- `app/src/main/java/com/android/purebilibili/feature/video/ui/pager/PortraitVideoPager.kt`
- `app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt`

**Acceptance**
- No dead/duplicated key action in portrait controls.
- Share text includes title + link.
- Rotate button reachable one-thumb on right side.
- Long-press speed behavior exists and does not conflict with existing gestures.

#### 2) Today Watch plugin usability + read-state refresh

**Scope**
- Add explicit usage guidance (card hint + first-use explanation entry).
- Mark watched/reviewed videos and support auto-filter or quick regenerate.
- Refresh/re-rank plan after meaningful watch completion.

**Primary files**
- `app/src/main/java/com/android/purebilibili/feature/home/HomeCategoryPage.kt`
- `app/src/main/java/com/android/purebilibili/feature/home/HomeViewModel.kt`
- `app/src/main/java/com/android/purebilibili/feature/home/TodayWatchPolicy.kt`
- `app/src/main/java/com/android/purebilibili/core/store/TodayWatchProfileStore.kt`

**Acceptance**
- User can understand “how to use” in-card within 10 seconds.
- Watched items do not make list stale; list can refresh/reorder with clear rule.
- Today Watch list remains useful after user inspects multiple entries.

### P1 (Second Wave)

#### 3) Top bar visual coordination tuning

**Scope**
- Reduce top tab background block size.
- Slightly increase text size while still below old-version scale.
- Keep motion and alignment stable across label modes.

**Primary files**
- `app/src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt`
- `app/src/main/java/com/android/purebilibili/feature/home/components/iOSHomeHeader.kt`
- `app/src/main/java/com/android/purebilibili/feature/home/components/TopTabStylePolicy.kt`

**Acceptance**
- Visual ratio between text and indicator/background is balanced.
- No new clipping/misalignment on compact/normal widths.
- A/B-compatible tokenized values for quick rollback.

### P2 (Third Wave)

#### 4) Top bar customization (show/hide/reorder)

**Scope**
- Add user-configurable top tab visibility and ordering.
- Persist and load custom top-tab schema.
- Add settings UI entry for manage/reorder.

**Primary files**
- `app/src/main/java/com/android/purebilibili/core/store/SettingsManager.kt`
- `app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt`
- `app/src/main/java/com/android/purebilibili/feature/home/HomeTopCategoryPolicy.kt`
- `app/src/main/java/com/android/purebilibili/feature/settings/BottomBarSettingsScreen.kt` (or dedicated settings page)

**Acceptance**
- Users can hide and reorder top tabs.
- Navigation, pager index mapping, and restore behavior remain correct.
- Works with existing top-tab label modes.

### Deferred Enhancements (After P0/P1 Stability)

- Portrait behavior parity with Bilibili for:
- Tap title to open intro/recommendations.
- Jump to UP page with continuous-play context.
- These should follow a separate interaction spec after baseline fix release.

### Rollout and Rollback Strategy

- Deliver each item as isolated commits/PRs.
- Gate risky behavior with settings flags (default conservative).
- Roll out in sequence: P0.1 -> P0.2 -> P1 -> P2.
- Rollback rule: revert latest unit if crash/gesture regression or KPI drop appears.

### Verification Matrix

- Unit tests for policy/state transitions (Today Watch ranking/filter and top-tab mapping).
- UI behavior checks for portrait controls and share payload content.
- Regression checks for predictive back, shared transitions, and full-screen toggles.
