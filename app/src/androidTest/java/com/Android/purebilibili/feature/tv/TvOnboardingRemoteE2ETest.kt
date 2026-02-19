package com.android.purebilibili.feature.tv

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.feature.onboarding.OnboardingScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class TvOnboardingRemoteE2ETest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tvRemote_canCompleteOnboardingWithDpadCenter() {
        composeRule.setContent {
            var finished by mutableStateOf(false)
            MaterialTheme {
                if (finished) {
                    Text("onboarding_finished_flag")
                } else {
                    OnboardingScreen(onFinish = { finished = true })
                }
            }
        }

        val root = composeRule.onNodeWithTag("onboarding_root")
        repeat(3) {
            root.performKeyInput { pressKey(Key.DirectionCenter) }
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithText("开始体验").assertExists()

        root.performKeyInput { pressKey(Key.DirectionCenter) }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("onboarding_finished_flag").assertExists()
    }
}
