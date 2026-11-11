package com.jigmnnaga.presentation.appointment.existingadd

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExistingAppointmentAddScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun overviewShowsRescheduleActionAndInvokesCallback() {
        var clickCount = 0
        composeRule.setContent {
            JigmnnagaTheme {
                ExistingAppointmentAddScreen(
                    state = testState(ExistingAppointmentAddPage.RESCHEDULE),
                    onBackClick = {}, onCalendarClick = {}, onPlaceClick = {}, onMoreClick = {},
                    onChatTabClick = {}, onMapAndStatusTabClick = {}, onMemberSelectionChange = { _, _ -> },
                    onRescheduleClick = { clickCount++ }, onRequestClick = {}
                )
            }
        }
        composeRule.onNodeWithText("실시간").assertExists()
        composeRule.onNodeWithText("다시 약속 잡기").performClick()
        composeRule.runOnIdle { assertEquals(1, clickCount) }
    }

    @Test
    fun participantSelectionIsExternalAndRequestEnabledFollowsUiState() {
        var toggledId = ""
        var toggledValue = false
        var requestCount = 0
        val state = mutableStateOf(testState(ExistingAppointmentAddPage.REQUEST_PARTICIPANTS))
        composeRule.setContent {
            JigmnnagaTheme {
                ExistingAppointmentAddScreen(
                    state = state.value,
                    onBackClick = {}, onCalendarClick = {}, onPlaceClick = {}, onMoreClick = {},
                    onChatTabClick = {}, onMapAndStatusTabClick = {},
                    onMemberSelectionChange = { id, selected -> toggledId = id; toggledValue = selected },
                    onRescheduleClick = {}, onRequestClick = { requestCount++ }
                )
            }
        }

        composeRule.onNodeWithContentDescription("태양, 참석").performClick()
        composeRule.runOnIdle {
            assertEquals("member-1", toggledId)
            assertTrue(toggledValue)
        }
        composeRule.onNodeWithText("약속 요청하기").assertIsNotEnabled().performClick()
        composeRule.runOnIdle { assertEquals(0, requestCount) }

        composeRule.runOnIdle { state.value = state.value.copy(canRequest = true) }
        composeRule.onNodeWithText("약속 요청하기").performClick()
        composeRule.runOnIdle { assertEquals(1, requestCount) }
    }

    @Test
    fun mapTabSelectionAndRoomHeaderActionsAreExposed() {
        var mapClicks = 0
        var backClicks = 0
        composeRule.setContent {
            JigmnnagaTheme {
                ExistingAppointmentAddScreen(
                    state = testState(ExistingAppointmentAddPage.REQUEST_PARTICIPANTS),
                    onBackClick = { backClicks++ }, onCalendarClick = {}, onPlaceClick = {}, onMoreClick = {},
                    onChatTabClick = {}, onMapAndStatusTabClick = { mapClicks++ },
                    onMemberSelectionChange = { _, _ -> }, onRescheduleClick = {}, onRequestClick = {}
                )
            }
        }
        composeRule.onNodeWithText("지도·현황").assertIsSelected().performClick()
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()
        composeRule.runOnIdle {
            assertEquals(1, mapClicks)
            assertEquals(1, backClicks)
        }
    }

    @Test
    fun memberTrailingValueAndCaptionAreShown() {
        composeRule.setContent {
            JigmnnagaTheme {
                ExistingAppointmentAddScreen(
                    state = testState(ExistingAppointmentAddPage.RESCHEDULE).copy(
                        members = listOf(
                            ExistingAppointmentMemberUiState(
                                id = "member-2",
                                name = "태양",
                                statusLabel = "도착",
                                detailLabel = "장소에 도착했어요",
                                trailingValue = "2분",
                                trailingCaption = "지각"
                            )
                        )
                    ),
                    onBackClick = {}, onCalendarClick = {}, onPlaceClick = {}, onMoreClick = {},
                    onChatTabClick = {}, onMapAndStatusTabClick = {}, onMemberSelectionChange = { _, _ -> },
                    onRescheduleClick = {}, onRequestClick = {}
                )
            }
        }
        composeRule.onNodeWithText("2분").assertExists()
        composeRule.onNodeWithText("지각").assertExists()
    }

    @Test
    fun requestScreenFitsNarrowWidthAtAccessibleFontScales() {
        val fontScale = mutableFloatStateOf(1f)
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScale.floatValue)) {
                JigmnnagaTheme {
                    Box(Modifier.width(320.dp)) {
                        ExistingAppointmentAddScreen(
                            state = testState(ExistingAppointmentAddPage.REQUEST_PARTICIPANTS).copy(canRequest = true),
                            onBackClick = {}, onCalendarClick = {}, onPlaceClick = {}, onMoreClick = {},
                            onChatTabClick = {}, onMapAndStatusTabClick = {}, onMemberSelectionChange = { _, _ -> },
                            onRescheduleClick = {}, onRequestClick = {}
                        )
                    }
                }
            }
        }

        listOf(1f, 1.3f, 1.5f).forEach { scale ->
            composeRule.runOnIdle { fontScale.floatValue = scale }
            composeRule.waitForIdle()
            composeRule.onNodeWithText("약속 요청하기").assertExists()
            composeRule.onNodeWithText("긴 이름도 화면에서 말줄임으로 처리", substring = true).assertExists()
        }
    }
}

private fun testState(page: ExistingAppointmentAddPage) = ExistingAppointmentAddUiState(
    page = page,
    appointmentTitle = "중간고사 카공 모임",
    memberSummary = "3명 · 태양, 하은, 나",
    scheduledTime = "오후 3:05",
    scheduledPlace = "강남역 가까운 곳",
    liveStatusLabel = "실시간",
    members = listOf(ExistingAppointmentMemberUiState("member-1", "태양", "참석", "긴 이름도 화면에서 말줄임으로 처리"))
)