# TV Split (Mobile + Tablet Only) Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Remove all TV UI/remote/Leanback adaptation from the current repository while preserving TV-token high-quality playback capability for mobile/tablet.

**Architecture:** Keep network/token/app-sign paths intact (TV token acquisition and playback signing), but delete TV runtime entry points (Leanback, DPAD/focus policies, TV-specific UI branches) and converge layout/motion policies to phone+tablet only. Execute in small commits with test gates after each slice.

**Tech Stack:** Android (Kotlin), Jetpack Compose, DataStore, Retrofit/OkHttp, Gradle, JUnit5/Kotlin test.

---

### Task 1: Create TV Repo Baseline Before Deletion

**Files:**
- Create: none (git-only task)
- Modify: none
- Test: none

**Step 1: Create split baseline branch from current main**

```bash
git checkout -b codex/tv-split-baseline
```

**Step 2: Materialize new TV repository from baseline**

```bash
cd /Users/yiyang/Desktop
git clone /Users/yiyang/Desktop/BiliPai BiliPai-TV
cd BiliPai-TV
git checkout codex/tv-split-baseline
```

**Step 3: Tag baseline in TV repository**

```bash
git tag tv-base-from-6.0.3
git tag --list | rg tv-base-from-6.0.3
```

Expected: prints `tv-base-from-6.0.3`.

**Step 4: Commit (TV repo only, if any metadata changed)**

```bash
git add .
git commit -m "chore(tv): mark split baseline from 6.0.3" || true
```

### Task 2: Add Guard Tests (Failing First)

**Files:**
- Create: `app/src/test/java/com/android/purebilibili/ManifestMobileOnlyConfigurationTest.kt`
- Create: `app/src/test/java/com/android/purebilibili/core/network/AppSignUtilsTvTokenFlowTest.kt`
- Delete: `app/src/test/java/com/android/purebilibili/ManifestTvConfigurationTest.kt`
- Test: `app/src/test/java/com/android/purebilibili/ManifestMobileOnlyConfigurationTest.kt`
- Test: `app/src/test/java/com/android/purebilibili/core/network/AppSignUtilsTvTokenFlowTest.kt`

**Step 1: Write failing manifest test (mobile/tablet only)**

```kotlin
package com.android.purebilibili

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse

class ManifestMobileOnlyConfigurationTest {
    @Test
    fun manifest_does_not_expose_leanback_entry() {
        val manifest = File("app/src/main/AndroidManifest.xml").readText()
        assertFalse(manifest.contains("android.software.leanback"))
        assertFalse(manifest.contains("android.intent.category.LEANBACK_LAUNCHER"))
        assertFalse(manifest.contains("android:banner="))
    }
}
```

**Step 2: Write failing token-chain guard test**

```kotlin
package com.android.purebilibili.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppSignUtilsTvTokenFlowTest {
    @Test
    fun tv_token_flow_keeps_tv_app_key_and_sign() {
        val signed = AppSignUtils.signForTvLogin(
            mapOf(
                "appkey" to AppSignUtils.TV_APP_KEY,
                "access_key" to "abc",
                "ts" to "1"
            )
        )
        assertEquals(AppSignUtils.TV_APP_KEY, signed["appkey"])
        assertTrue(!signed["sign"].isNullOrBlank())
    }
}
```

**Step 3: Run tests to verify first one fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.android.purebilibili.ManifestMobileOnlyConfigurationTest -q`
Expected: FAIL (current manifest still contains Leanback).

**Step 4: Commit test scaffold**

```bash
git add app/src/test/java/com/android/purebilibili/ManifestMobileOnlyConfigurationTest.kt app/src/test/java/com/android/purebilibili/core/network/AppSignUtilsTvTokenFlowTest.kt app/src/test/java/com/android/purebilibili/ManifestTvConfigurationTest.kt
git commit -m "test(split): add mobile-only manifest and token guard tests"
```

### Task 3: Remove Leanback/TV Launcher Manifest Surface

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `app/src/test/java/com/android/purebilibili/ManifestMobileOnlyConfigurationTest.kt`

**Step 1: Remove Leanback features and launcher category**

```xml
<!-- delete -->
<uses-feature android:name="android.software.leanback" android:required="false" />

<!-- delete -->
<category android:name="android.intent.category.LEANBACK_LAUNCHER" />
```

**Step 2: Remove app banner attribute**

```xml
<!-- delete from <application ...> -->
android:banner="@mipmap/ic_launcher_tv"
```

**Step 3: Re-run manifest guard test**

Run: `./gradlew :app:testDebugUnitTest --tests com.android.purebilibili.ManifestMobileOnlyConfigurationTest -q`
Expected: PASS.

**Step 4: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "refactor(split): remove leanback launcher and TV banner from manifest"
```

### Task 4: Delete TV Policy Source + TV-Specific Tests

**Files:**
- Delete: `app/src/main/java/com/android/purebilibili/feature/onboarding/OnboardingTvNavigationPolicy.kt`
- Delete: `app/src/main/java/com/android/purebilibili/feature/search/SearchTvFocusPolicy.kt`
- Delete: `app/src/main/java/com/android/purebilibili/feature/home/HomeTvFocusPolicy.kt`
- Delete: `app/src/main/java/com/android/purebilibili/feature/settings/SettingsTvFocusPolicy.kt`
- Delete: `app/src/main/java/com/android/purebilibili/feature/home/components/SideBarTvKeyPolicy.kt`
- Delete: `app/src/main/java/com/android/purebilibili/feature/home/components/TopBarTvKeyPolicy.kt`
- Delete: `app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarTvKeyPolicy.kt`
- Delete: `app/src/main/java/com/android/purebilibili/feature/home/components/cards/HomeCardTvKeyPolicy.kt`
- Delete: `app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailTvFocusPolicy.kt`
- Delete: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/VideoOverlayTvNavigationPolicy.kt`
- Delete: `app/src/main/java/com/android/purebilibili/core/ui/animation/TvFocusJiggleModifier.kt`
- Delete: `app/src/main/java/com/android/purebilibili/core/ui/animation/TvFocusJiggleSpec.kt`
- Delete: `app/src/test/java/com/android/purebilibili/feature/onboarding/OnboardingTvNavigationPolicyTest.kt`
- Delete: `app/src/test/java/com/android/purebilibili/feature/search/SearchTvFocusPolicyTest.kt`
- Delete: `app/src/test/java/com/android/purebilibili/feature/video/screen/VideoDetailTvFocusPolicyTest.kt`
- Delete: `app/src/test/java/com/android/purebilibili/feature/video/screen/VideoContentTvFocusPolicyTest.kt`
- Delete: `app/src/test/java/com/android/purebilibili/feature/settings/SettingsTvFocusPolicyTest.kt`
- Delete: `app/src/test/java/com/android/purebilibili/feature/home/HomeTvFocusPolicyTest.kt`
- Delete: `app/src/test/java/com/android/purebilibili/feature/home/components/TopBarTvKeyPolicyTest.kt`
- Delete: `app/src/test/java/com/android/purebilibili/feature/home/components/SideBarTvKeyPolicyTest.kt`
- Delete: `app/src/test/java/com/android/purebilibili/feature/home/components/BottomBarTvKeyPolicyTest.kt`
- Delete: `app/src/test/java/com/android/purebilibili/feature/home/components/cards/HomeCardTvKeyPolicyTest.kt`
- Delete: `app/src/test/java/com/android/purebilibili/feature/video/ui/overlay/VideoOverlayTvNavigationPolicyTest.kt`
- Delete: `app/src/test/java/com/android/purebilibili/core/ui/animation/TvFocusJiggleSpecTest.kt`
- Delete: `app/src/test/java/com/android/purebilibili/core/util/TvDevicePolicyTest.kt`

**Step 1: Delete TV policy files and TV unit tests**

```bash
git rm app/src/main/java/com/android/purebilibili/feature/onboarding/OnboardingTvNavigationPolicy.kt \
  app/src/main/java/com/android/purebilibili/feature/search/SearchTvFocusPolicy.kt \
  app/src/main/java/com/android/purebilibili/feature/home/HomeTvFocusPolicy.kt \
  app/src/main/java/com/android/purebilibili/feature/settings/SettingsTvFocusPolicy.kt \
  app/src/main/java/com/android/purebilibili/feature/home/components/SideBarTvKeyPolicy.kt \
  app/src/main/java/com/android/purebilibili/feature/home/components/TopBarTvKeyPolicy.kt \
  app/src/main/java/com/android/purebilibili/feature/home/components/BottomBarTvKeyPolicy.kt \
  app/src/main/java/com/android/purebilibili/feature/home/components/cards/HomeCardTvKeyPolicy.kt \
  app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailTvFocusPolicy.kt \
  app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/VideoOverlayTvNavigationPolicy.kt \
  app/src/main/java/com/android/purebilibili/core/ui/animation/TvFocusJiggleModifier.kt \
  app/src/main/java/com/android/purebilibili/core/ui/animation/TvFocusJiggleSpec.kt
```

**Step 2: Remove TV unit tests**

```bash
git rm app/src/test/java/com/android/purebilibili/feature/onboarding/OnboardingTvNavigationPolicyTest.kt \
  app/src/test/java/com/android/purebilibili/feature/search/SearchTvFocusPolicyTest.kt \
  app/src/test/java/com/android/purebilibili/feature/video/screen/VideoDetailTvFocusPolicyTest.kt \
  app/src/test/java/com/android/purebilibili/feature/video/screen/VideoContentTvFocusPolicyTest.kt \
  app/src/test/java/com/android/purebilibili/feature/settings/SettingsTvFocusPolicyTest.kt \
  app/src/test/java/com/android/purebilibili/feature/home/HomeTvFocusPolicyTest.kt \
  app/src/test/java/com/android/purebilibili/feature/home/components/TopBarTvKeyPolicyTest.kt \
  app/src/test/java/com/android/purebilibili/feature/home/components/SideBarTvKeyPolicyTest.kt \
  app/src/test/java/com/android/purebilibili/feature/home/components/BottomBarTvKeyPolicyTest.kt \
  app/src/test/java/com/android/purebilibili/feature/home/components/cards/HomeCardTvKeyPolicyTest.kt \
  app/src/test/java/com/android/purebilibili/feature/video/ui/overlay/VideoOverlayTvNavigationPolicyTest.kt \
  app/src/test/java/com/android/purebilibili/core/ui/animation/TvFocusJiggleSpecTest.kt \
  app/src/test/java/com/android/purebilibili/core/util/TvDevicePolicyTest.kt
```

**Step 3: Compile-only gate**

Run: `./gradlew :app:compileDebugKotlin --no-daemon`
Expected: SUCCESS (then fix missing references in next tasks).

**Step 4: Commit**

```bash
git commit -m "refactor(split): remove tv focus, dpad, and animation policies" -a
```

### Task 5: Remove TV Device Detection + TV Performance Profile

**Files:**
- Delete: `app/src/main/java/com/android/purebilibili/core/util/TvDevicePolicy.kt`
- Modify: `app/src/main/java/com/android/purebilibili/core/ui/adaptive/DeviceUiProfile.kt`
- Modify: `app/src/main/java/com/android/purebilibili/core/store/SettingsManager.kt`
- Modify: `app/src/main/java/com/android/purebilibili/app/PureApplication.kt`
- Test: `app/src/test/java/com/android/purebilibili/core/network/AppSignUtilsTvTokenFlowTest.kt`

**Step 1: Remove TV utility API**

```bash
git rm app/src/main/java/com/android/purebilibili/core/util/TvDevicePolicy.kt
```

**Step 2: Converge DeviceUiProfile to mobile/tablet**

```kotlin
fun resolveDeviceUiProfile(
    widthSizeClass: WindowWidthSizeClass
): DeviceUiProfile {
    val isTablet = widthSizeClass != WindowWidthSizeClass.Compact
    val motionTier = if (widthSizeClass == WindowWidthSizeClass.Expanded) {
        MotionTier.Enhanced
    } else {
        MotionTier.Normal
    }
    return DeviceUiProfile(
        widthSizeClass = widthSizeClass,
        isTablet = isTablet,
        isTv = false,
        isTenFootUi = false,
        motionTier = motionTier
    )
}
```

**Step 3: Remove SettingsManager TV performance settings**

```kotlin
// delete KEY_TV_PERFORMANCE_PROFILE_ENABLED and these APIs:
// getTvPerformanceProfileEnabled(...)
// setTvPerformanceProfileEnabled(...)
// getTvPerformanceProfileEnabledSync(...)
```

**Step 4: Replace app-level cache sizing with non-TV defaults**

```kotlin
val memoryCachePercent = 0.30
val diskCacheBytes = 512L * 1024L * 1024L
```

**Step 5: Run target tests**

Run: `./gradlew :app:testDebugUnitTest --tests com.android.purebilibili.core.network.AppSignUtilsTvTokenFlowTest -q`
Expected: PASS.

**Step 6: Commit**

```bash
git add app/src/main/java/com/android/purebilibili/core/ui/adaptive/DeviceUiProfile.kt app/src/main/java/com/android/purebilibili/core/store/SettingsManager.kt app/src/main/java/com/android/purebilibili/app/PureApplication.kt
git commit -m "refactor(split): remove tv device detection and tv performance profile"
```

### Task 6: Remove TV Branches in Home/Search/Settings/List/Category/WatchLater

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/feature/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/search/SearchScreen.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/settings/PlaybackSettingsScreen.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/settings/AppearanceSettingsScreen.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/settings/AnimationSettingsScreen.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/settings/BottomBarSettingsScreen.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/settings/PermissionSettingsScreen.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/settings/TipsSettingsScreen.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/settings/TabletSettingsLayout.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/category/CategoryScreen.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/watchlater/WatchLaterScreen.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/home/components/TopBar.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/home/components/SideBar.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/home/components/MineSideDrawer.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/home/components/cards/StoryVideoCard.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/home/components/cards/GlassVideoCard.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/home/components/cards/CinematicVideoCard.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/home/components/cards/LiveRoomCard.kt`

**Step 1: Replace `rememberIsTvDevice()`/`isTvDevice` usage with constants or width-class logic**

```kotlin
// before
val isTvDevice = rememberIsTvDevice()

// after
val isTvDevice = false
```

**Step 2: Remove `getTvPerformanceProfileEnabled(...)` flows and pass width-only profile**

```kotlin
val deviceUiProfile = remember(windowSizeClass.widthSizeClass) {
    resolveDeviceUiProfile(widthSizeClass = windowSizeClass.widthSizeClass)
}
```

**Step 3: Run focused UI/unit tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.search.*" --tests "com.android.purebilibili.feature.settings.*"`
Expected: PASS.

**Step 4: Commit**

```bash
git add app/src/main/java/com/android/purebilibili/feature/home app/src/main/java/com/android/purebilibili/feature/search app/src/main/java/com/android/purebilibili/feature/settings app/src/main/java/com/android/purebilibili/feature/category app/src/main/java/com/android/purebilibili/feature/list app/src/main/java/com/android/purebilibili/feature/watchlater
git commit -m "refactor(split): remove tv branches from home/search/settings/list flows"
```

### Task 7: Remove TV Branches in Video Detail + Overlay

**Files:**
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/screen/VideoDetailScreen.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/screen/TabletVideoLayout.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/screen/TabletCinemaLayout.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/section/VideoPlayerSection.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/section/VideoPlayerUiLayoutPolicy.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/VideoPlayerOverlay.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/BottomControlBar.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/BottomRightControls.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/TopControlBar.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/PortraitBottomInputBar.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/PortraitInteractionBar.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/PortraitProgressBar.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/PortraitFullscreenOverlay.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/MiniPlayerOverlay.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/VideoPlayerOverlayVisualPolicy.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/BottomControlBarLayoutPolicy.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/BottomRightControlsLayoutPolicy.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/LandscapeEndDrawerLayoutPolicy.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/MiniPlayerOverlayLayoutPolicy.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/PortraitBottomInputBarLayoutPolicy.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/PortraitFullscreenOverlayLayoutPolicy.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/PortraitInteractionBarLayoutPolicy.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/PortraitProgressBarLayoutPolicy.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/PortraitTopBarLayoutPolicy.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/TopControlBarLayoutPolicy.kt`
- Modify: `app/src/main/java/com/android/purebilibili/feature/video/ui/overlay/VideoProgressBarLayoutPolicy.kt`

**Step 1: Remove overlay/section `isTv` and focus-requester paths**

```kotlin
// before
if (isTvDevice) {
    Modifier.focusRequester(tvOverlayFocusRequester).focusable()
}

// after
Modifier
```

**Step 2: Convert policy functions to width-only logic**

```kotlin
// before
fun resolveTopControlBarLayoutPolicy(widthDp: Int, isTv: Boolean): TopControlBarLayoutPolicy

// after
fun resolveTopControlBarLayoutPolicy(widthDp: Int): TopControlBarLayoutPolicy
```

**Step 3: Run video-related unit tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.purebilibili.feature.video.*"`
Expected: PASS.

**Step 4: Commit**

```bash
git add app/src/main/java/com/android/purebilibili/feature/video
git commit -m "refactor(split): remove tv branches from video detail and overlay"
```

### Task 8: Remove TV AndroidTests and TV Scripts

**Files:**
- Delete: `app/src/androidTest/java/com/Android/purebilibili/feature/tv/TvHomeFocusNavigationTest.kt`
- Delete: `app/src/androidTest/java/com/Android/purebilibili/feature/tv/TvPerformanceProfileToggleTest.kt`
- Delete: `app/src/androidTest/java/com/Android/purebilibili/feature/tv/TvVideoBackRegressionAndroidTest.kt`
- Delete: `app/src/androidTest/java/com/Android/purebilibili/feature/tv/TvSearchSettingsEndToEndTest.kt`
- Delete: `app/src/androidTest/java/com/Android/purebilibili/feature/tv/TvFocusNavigationTest.kt`
- Delete: `app/src/androidTest/java/com/Android/purebilibili/feature/tv/TvOnboardingRemoteE2ETest.kt`
- Delete: `scripts/tv_perf_collect.sh`
- Delete: `scripts/tv_remote_one_click.sh`

**Step 1: Delete Android TV tests**

```bash
git rm app/src/androidTest/java/com/Android/purebilibili/feature/tv/*.kt
```

**Step 2: Delete TV scripts**

```bash
git rm scripts/tv_perf_collect.sh scripts/tv_remote_one_click.sh
```

**Step 3: Compile + unit test gate**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

**Step 4: Commit**

```bash
git commit -m "test(split): remove tv android tests and tv helper scripts" -a
```

### Task 9: Update README/CHANGELOG and Final Verification

**Files:**
- Modify: `README.md`
- Modify: `README_EN.md`
- Modify: `CHANGELOG.md`

**Step 1: Add repository-boundary statement**

```markdown
- 当前仓库维护范围：移动端 + 平板端
- TV 端代码维护仓库：BiliPai-TV
- 高画质 token 能力：仍在当前仓库保留
```

**Step 2: Run full verification gate (@superpowers/verification-before-completion)**

Run:
- `./gradlew :app:assembleDebug`
- `./gradlew :app:assembleRelease`
- `./gradlew :app:testDebugUnitTest`

Expected: all SUCCESS.

**Step 3: Final commit**

```bash
git add README.md README_EN.md CHANGELOG.md
git commit -m "docs(split): document mobile/tablet-only scope and tv repo ownership"
```

### Task 10: Merge-Readiness Check

**Files:**
- Modify: none (verification task)
- Test: all modified modules

**Step 1: Confirm no Leanback remnants in current repository**

Run: `rg -n "LEANBACK|leanback|rememberIsTvDevice|tvPerformanceProfile" app/src/main app/src/test app/src/androidTest`
Expected: no matches in removed scope (except token-related comments/strings if intentionally kept).

**Step 2: Confirm token chain references remain**

Run: `rg -n "TV_APP_KEY|signForTvLogin|generateTvQrCode|pollTvQrCode|refreshToken" app/src/main/java`
Expected: matches remain in login/network/repository only.

**Step 3: Commit merge note (optional)**

```bash
git commit --allow-empty -m "chore(split): verification complete for tv extraction"
```

