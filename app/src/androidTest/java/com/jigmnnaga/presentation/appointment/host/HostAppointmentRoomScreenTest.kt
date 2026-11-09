package com.jigmnnaga.presentation.appointment.host

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import org.junit.Rule
import org.junit.Test

class HostAppointmentRoomScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun displaysRoomSummaryAndChatInput() {
        composeRule.setContent {
            JigmnnagaTheme {
                HostAppointmentRoomScreen(
                    state = testState(),
                    onBackClick = {},
                    onCalendarClick = {},
                    onPlaceClick = {},
                    onMoreClick = {},
                    onTabSelected = {},
                    onSummaryExpandedChange = {},
                    onMessageChange = {},
                    onSendMessage = {}
                )
            }
        }

        composeRule.onNodeWithText("중간고사 카공 모임").assertIsDisplayed()
        composeRule.onNodeWithText("약속 정보").assertIsDisplayed()
        composeRule.onNodeWithTag("host-room-chat-input").assertIsDisplayed()
        composeRule.onNodeWithTag("host-room-tab-chat").assertIsSelected()
    }

    @Test
    fun mapStatusTabInvokesSelectionCallback() {
        val currentState = mutableStateOf(testState())
        composeRule.setContent {
            JigmnnagaTheme {
                HostAppointmentRoomScreen(
                    state = currentState.value,
                    onBackClick = {},
                    onCalendarClick = {},
                    onPlaceClick = {},
                    onMoreClick = {},
                    onTabSelected = { tab -> currentState.value = currentState.value.copy(selectedTab = tab) },
                    onSummaryExpandedChange = {},
                    onMessageChange = {},
                    onSendMessage = {}
                )
            }
        }

        composeRule.onNodeWithTag("host-room-tab-map").performClick()
        composeRule.onNodeWithTag("host-room-tab-map").assertIsSelected()
        composeRule.onNodeWithText("지도 SDK 연결 영역").assertIsDisplayed()
        composeRule.onNodeWithText("참여 멤버 현황").assertIsDisplayed()
        composeRule.onNodeWithText("태희").assertIsDisplayed()
        composeRule.onNodeWithText("선택 완료 · 2명").assertIsDisplayed()
        composeRule.onNodeWithText("미응답 · 1명").assertIsDisplayed()
    }

    @Test
    fun narrowWidthAndLargeFontKeepMainControlsVisible() {
        val fontScales = listOf(1f, 1.3f, 1.5f)
        val fontScale = mutableFloatStateOf(1f)
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(baseDensity.density, fontScale.floatValue)
            ) {
                JigmnnagaTheme {
                    Box(Modifier.width(320.dp)) {
                        HostAppointmentRoomScreen(
                            state = testState().copy(isSummaryExpanded = true),
                            onBackClick = {},
                            onCalendarClick = {},
                            onPlaceClick = {},
                            onMoreClick = {},
                            onTabSelected = {},
                            onSummaryExpandedChange = {},
                            onMessageChange = {},
                            onSendMessage = {}
                        )
                    }
                }
            }
        }

        fontScales.forEach { scale ->
            composeRule.runOnIdle {
                fontScale.floatValue = scale
            }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("host-appointment-room").assertIsDisplayed()
            composeRule.onNodeWithTag("host-room-chat-input").assertIsDisplayed()
            composeRule.onNodeWithText("9월 4일–9월 11일").assertIsDisplayed()
        }
    }

    private fun testState() = HostAppointmentRoomUiState(
        title = "중간고사 카공 모임",
        subtitle = "3명 · 약속방",
        scheduleStatusLabel = "확정 전",
        dateSummary = "9월 4일–9월 11일",
        placeSummary = "강남역, 잠실역",
        responseCount = 2,
        participantCount = 3,
        participants = listOf(
            HostRoomParticipantUiState("host", "나", isHost = true),
            HostRoomParticipantUiState("member", "태희")
        ),
        messages = listOf(HostRoomMessageUiState("message", "태희", "저는 오후 6시 이후면 괜찮아요!"))
    )
}
