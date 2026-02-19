package com.android.purebilibili.feature.tv

import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.util.LocalWindowSizeClass
import com.android.purebilibili.core.util.WindowHeightSizeClass
import com.android.purebilibili.core.util.WindowSizeClass
import com.android.purebilibili.core.util.WindowWidthSizeClass
import com.android.purebilibili.feature.search.SearchTopBar
import com.android.purebilibili.feature.settings.SettingsCategory
import com.android.purebilibili.feature.settings.TabletSettingsLayout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class TvSearchSettingsEndToEndTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchTopBar_canMoveDownToSuggestionAndBackWithDpad() {
        composeRule.setContent {
            MaterialTheme {
                SearchTvE2EHost()
            }
        }

        val input = composeRule.onNode(hasSetTextAction())
        input.performClick()
        input.performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionDown) }
        composeRule.onNodeWithTag("search_suggestion_0").assertIsFocused()

        composeRule.onNodeWithTag("search_suggestion_0")
            .performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionUp) }
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
    }

    @Test
    fun settingsTabletLayout_canMoveBetweenCategoryAndDetailByDpad() {
        setSettingsTabletLayoutContent()

        composeRule.onNodeWithTag("settings_category_GENERAL").performClick()
        composeRule.onNodeWithTag("settings_category_GENERAL")
            .performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionRight) }
        composeRule.onNodeWithTag("settings_detail_panel").assertIsFocused()

        composeRule.onNodeWithTag("settings_detail_panel")
            .performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionLeft) }
        composeRule.onNodeWithTag("settings_category_GENERAL").assertIsFocused()
    }

    @Test
    fun settingsTabletLayout_backFromDetail_returnsCategoryFocus() {
        setSettingsTabletLayoutContent()

        composeRule.onNodeWithTag("settings_category_GENERAL").performClick()
        composeRule.onNodeWithTag("settings_category_GENERAL")
            .performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionRight) }
        composeRule.onNodeWithTag("settings_detail_panel").assertIsFocused()

        composeRule.onNodeWithTag("settings_detail_panel")
            .performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Back) }
        composeRule.onNodeWithTag("settings_category_GENERAL").assertIsFocused()
    }

    private fun setSettingsTabletLayoutContent() {
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalWindowSizeClass provides WindowSizeClass(
                        widthSizeClass = WindowWidthSizeClass.Expanded,
                        heightSizeClass = WindowHeightSizeClass.Expanded,
                        widthDp = 1920.dp,
                        heightDp = 1080.dp
                    )
                ) {
                    TabletSettingsLayout(
                        onBack = {},
                        onAppearanceClick = {},
                        onPlaybackClick = {},
                        onPermissionClick = {},
                        onPluginsClick = {},
                        onExportLogsClick = {},
                        onLicenseClick = {},
                        onDisclaimerClick = {},
                        onGithubClick = {},
                        onCheckUpdateClick = {},
                        onVersionClick = {},
                        onReplayOnboardingClick = {},
                        onTelegramClick = {},
                        onTwitterClick = {},
                        onDownloadPathClick = {},
                        onClearCacheClick = {},
                        onDonateClick = {},
                        onTipsClick = {},
                        onOpenLinksClick = {},
                        onBlockedListClick = {},
                        onPrivacyModeChange = {},
                        onCrashTrackingChange = {},
                        onAnalyticsChange = {},
                        onEasterEggChange = {},
                        privacyModeEnabled = false,
                        customDownloadPath = null,
                        cacheSize = "0 MB",
                        crashTrackingEnabled = true,
                        analyticsEnabled = true,
                        pluginCount = 0,
                        versionName = "5.3.4",
                        versionClickCount = 0,
                        versionClickThreshold = 7,
                        easterEggEnabled = false,
                        updateStatusText = "点击检查",
                        isCheckingUpdate = false,
                        feedApiType = com.android.purebilibili.core.store.SettingsManager.FeedApiType.WEB,
                        onFeedApiTypeChange = {},
                        incrementalTimelineRefreshEnabled = false,
                        onIncrementalTimelineRefreshChange = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchTvE2EHost() {
    val topFocusRequester = remember { FocusRequester() }
    val suggestionFocusRequester = remember { FocusRequester() }
    var query by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        SearchTopBar(
            query = query,
            onBack = {},
            onQueryChange = { query = it },
            onSearch = {},
            onClearQuery = { query = "" },
            placeholder = "搜索视频、UP主...",
            focusRequester = topFocusRequester,
            onTvMoveFocusDown = { suggestionFocusRequester.requestFocus() }
        )

        Text(
            text = "suggestion_0",
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("search_suggestion_0")
                .focusRequester(suggestionFocusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP &&
                        event.nativeKeyEvent.action == KeyEvent.ACTION_UP
                    ) {
                        topFocusRequester.requestFocus()
                        true
                    } else {
                        false
                    }
                }
        )
    }
}
