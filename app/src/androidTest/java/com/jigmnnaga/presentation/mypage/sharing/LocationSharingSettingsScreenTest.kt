package com.jigmnnaga.presentation.mypage.sharing

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationSharingSettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setScreen(
        state: LocationSharingSettingsUiState = LocationSharingSettingsUiState(),
        onEnabledChange: (Boolean) -> Unit = {},
        onScope: (LocationSharingScope) -> Unit = {},
        onTime: (LocationSharingStartTime) -> Unit = {}
    ) {
        composeRule.setContent {
            JigmnnagaTheme {
                LocationSharingSettingsScreen(
                    state = state,
                    onBackClick = {},
                    onSharingEnabledChange = onEnabledChange,
                    onScopeSelect = onScope,
                    onStartTimeSelect = onTime
                )
            }
        }
    }

    @Test
    fun showsEverySectionOfTheFigmaScreen() {
        setScreen()
        listOf(
            "위치 공유 설정",
            "현재 위치 권한",
            "앱 사용 중 위치 접근이 허용되어 있어요",
            "허용됨",
            "친구와 위치 공유",
            "위치 공유 사용",
            "공유 범위",
            "약속에 참여한 친구만",
            "위치 공유 안 함",
            "공유 시작 시간",
            "약속 10분 전",
            "약속 30분 전",
            "약속 1시간 전"
        ).forEach { composeRule.onNodeWithText(it).assertExists() }
        composeRule.onNodeWithTag("location-sharing-notice").performScrollTo().assertExists()
        composeRule.onNodeWithText("위치는 안전하게 보호돼요").assertExists()
    }

    @Test
    fun defaultStateMatchesFigma() {
        setScreen()
        composeRule.onNodeWithTag("location-sharing-switch").assertIsOn()
        composeRule.onNodeWithTag("location-sharing-scope-FRIENDS_ONLY").assertIsSelected()
        composeRule.onNodeWithTag("location-sharing-scope-NONE").assertIsNotSelected()
        composeRule.onNodeWithTag("location-sharing-time-MINUTES_30").assertIsSelected()
        composeRule.onNodeWithTag("location-sharing-time-MINUTES_10").assertIsNotSelected()
        composeRule.onNodeWithTag("location-sharing-time-HOUR_1").assertIsNotSelected()
    }

    @Test
    fun reflectsGivenState() {
        setScreen(
            LocationSharingSettingsUiState(
                sharingEnabled = false,
                scope = LocationSharingScope.NONE,
                startTime = LocationSharingStartTime.HOUR_1
            )
        )
        composeRule.onNodeWithTag("location-sharing-switch").assertIsOff()
        composeRule.onNodeWithTag("location-sharing-scope-NONE").assertIsSelected()
        composeRule.onNodeWithTag("location-sharing-scope-FRIENDS_ONLY").assertIsNotSelected()
        composeRule.onNodeWithTag("location-sharing-time-HOUR_1").assertIsSelected()
    }

    @Test
    fun tappingSwitchReportsOppositeValue() {
        var changed: Boolean? = null
        setScreen(onEnabledChange = { changed = it })
        composeRule.onNodeWithTag("location-sharing-switch").performClick()
        assertEquals(false, changed)
    }

    @Test
    fun tappingScopeAndTimeReportsSelection() {
        var scope: LocationSharingScope? = null
        var time: LocationSharingStartTime? = null
        setScreen(onScope = { scope = it }, onTime = { time = it })
        composeRule.onNodeWithTag("location-sharing-scope-NONE").performClick()
        composeRule.onNodeWithTag("location-sharing-time-MINUTES_10").performScrollTo().performClick()
        assertEquals(LocationSharingScope.NONE, scope)
        assertEquals(LocationSharingStartTime.MINUTES_10, time)
    }
}
