package com.jigmnnaga.presentation.appointment.participant

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParticipantAppointmentRoomScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun participantCanSwitchChatAndMapStatusTabsThroughCallbacks() {
        val selectedTab = mutableStateOf(ParticipantAppointmentRoomTab.CHAT)
        composeRule.setContent {
            JigmnnagaTheme {
                ParticipantAppointmentRoomScreen(
                    state = roomState(selectedTab = selectedTab.value),
                    onBackClick = {}, onCalendarClick = {}, onPlaceClick = {}, onManagementClick = {},
                    onTabSelected = { selectedTab.value = it }, onSummaryExpandedChange = {},
                    onMessageChange = {}, onSendMessage = {}, onDepartureClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("participant-tab-채팅").assertIsSelected().assertHasClickAction()
        composeRule.onNodeWithTag("participant-tab-지도·현황").assertIsNotSelected().performClick()
        composeRule.runOnIdle { assertEquals(ParticipantAppointmentRoomTab.MAP_STATUS, selectedTab.value) }
    }

    @Test
    fun summaryExpansionIsReportedToCaller() {
        var expanded: Boolean? = null
        composeRule.setContent {
            JigmnnagaTheme {
                ParticipantAppointmentRoomScreen(
                    state = roomState(),
                    onBackClick = {}, onCalendarClick = {}, onPlaceClick = {}, onManagementClick = {},
                    onTabSelected = {}, onSummaryExpandedChange = { expanded = it },
                    onMessageChange = {}, onSendMessage = {}, onDepartureClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("participant-room-summary-toggle").performClick()
        composeRule.runOnIdle { assertEquals(true, expanded) }
    }

    @Test
    fun confirmedRoomShowsParticipantDepartureActionOnlyWhenProvidedByState() {
        var departureClicks = 0
        composeRule.setContent {
            JigmnnagaTheme {
                ParticipantAppointmentRoomScreen(
                    state = roomState(
                        appointmentStatus = ParticipantAppointmentStatus.CONFIRMED,
                        canReportDeparture = true,
                        selectedTab = ParticipantAppointmentRoomTab.MAP_STATUS
                    ),
                    onBackClick = {}, onCalendarClick = {}, onPlaceClick = {}, onManagementClick = {},
                    onTabSelected = {}, onSummaryExpandedChange = {}, onMessageChange = {},
                    onSendMessage = {}, onDepartureClick = { departureClicks++ }
                )
            }
        }

        composeRule.onNodeWithText("지금 나가기").assertHasClickAction().performClick()
        composeRule.runOnIdle { assertEquals(1, departureClicks) }
    }

    @Test
    fun managementScreenExposesParticipantSettingsAndCallbacks() {
        var changedNotificationState: Boolean? = null
        var leaveCount = 0
        composeRule.setContent {
            JigmnnagaTheme {
                ParticipantAppointmentManagementScreen(
                    state = ParticipantAppointmentManagementUiState(
                        appointmentName = "저녁 모임",
                        scheduleStatusLabel = "확정 전",
                        members = listOf(ParticipantRoomMemberUiState("me", "나", isCurrentUser = true)),
                        myAvailabilitySummary = "토요일 오후",
                        isNotificationsEnabled = true
                    ),
                    onBackClick = {}, onEditAvailabilityClick = {},
                    onNotificationsEnabledChange = { changedNotificationState = it },
                    onLeaveAppointmentClick = { leaveCount++ }
                )
            }
        }

        composeRule.onNodeWithText("내 참여 설정").assertExists()
        composeRule.onNodeWithText("희망 날짜·시간 수정").assertHasClickAction()
        composeRule.onNodeWithText("약속방 나가기").performClick()
        composeRule.runOnIdle { assertEquals(1, leaveCount) }
        composeRule.onNodeWithTag("participant-room-notifications").performClick()
        composeRule.runOnIdle { assertEquals(false, changedNotificationState) }
    }

    @Test
    fun roomRemainsReadableIn320DpAtFontScalesOneToOnePointFive() {
        val fontScale = mutableStateOf(1f)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.value)) {
                JigmnnagaTheme {
                    Box(Modifier.width(320.dp)) {
                        ParticipantAppointmentRoomScreen(
                            state = roomState(
                                title = "매우 긴 약속 이름이 들어오는 경우 화면에서 말줄임 처리되어야 합니다",
                                subtitle = "참여자 이름과 상태가 길어도 레이아웃이 유지됩니다",
                                messages = listOf(
                                    ParticipantRoomMessageUiState("m1", "긴 채팅 문구도 화면 너비 안에서 줄 바꿈되어야 합니다.")
                                ),
                                members = listOf(
                                    ParticipantRoomMemberUiState("m1", "매우 긴 참여자 이름을 가진 사용자", "약속 일정 선택을 아직 완료하지 않았습니다")
                                )
                            ),
                            onBackClick = {}, onCalendarClick = {}, onPlaceClick = {}, onManagementClick = {},
                            onTabSelected = {}, onSummaryExpandedChange = {}, onMessageChange = {},
                            onSendMessage = {}, onDepartureClick = {}
                        )
                    }
                }
            }
        }

        listOf(1f, 1.3f, 1.5f).forEach { scale ->
            composeRule.runOnIdle { fontScale.value = scale }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("participant-appointment-room").assertExists()
            composeRule.onNodeWithText("약속 요약").assertExists()
        }
    }

    @Test
    fun managementScreenKeepsCoreActionsAvailableIn320DpAtLargeFontScales() {
        val fontScale = mutableStateOf(1f)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.value)) {
                JigmnnagaTheme {
                    Box(Modifier.width(320.dp)) {
                        ParticipantAppointmentManagementScreen(
                            state = ParticipantAppointmentManagementUiState(
                                appointmentName = "매우 긴 약속 이름이 화면 너비를 넘는 상황에서도 읽혀야 합니다",
                                scheduleStatusLabel = "아직 확정 전입니다",
                                members = listOf(
                                    ParticipantRoomMemberUiState(
                                        "me", "매우 긴 참여자 이름을 가진 사용자",
                                        "희망 날짜와 시간을 아직 선택하지 않았습니다", true
                                    )
                                ),
                                myAvailabilitySummary = "토요일 오후 시간대 중 선택한 긴 응답 요약"
                            ),
                            onBackClick = {}, onEditAvailabilityClick = {},
                            onNotificationsEnabledChange = {}, onLeaveAppointmentClick = {}
                        )
                    }
                }
            }
        }

        listOf(1f, 1.3f, 1.5f).forEach { scale ->
            composeRule.runOnIdle { fontScale.value = scale }
            composeRule.waitForIdle()
            composeRule.onNodeWithText("약속방 관리").assertExists()
            composeRule.onNodeWithText("약속방 나가기").assertExists()
        }
    }

    private fun roomState(
        title: String = "주말 약속",
        subtitle: String = "3명 · 참여자 약속방",
        appointmentStatus: ParticipantAppointmentStatus = ParticipantAppointmentStatus.AWAITING_CONFIRMATION,
        canReportDeparture: Boolean = false,
        selectedTab: ParticipantAppointmentRoomTab = ParticipantAppointmentRoomTab.CHAT,
        messages: List<ParticipantRoomMessageUiState> = emptyList(),
        members: List<ParticipantRoomMemberUiState> = emptyList()
    ) = ParticipantAppointmentRoomUiState(
        title = title,
        subtitle = subtitle,
        appointmentStatus = appointmentStatus,
        scheduleStatusLabel = "확정 전",
        dateSummary = "확정 전",
        placeSummary = "확정 전",
        members = members,
        messages = messages,
        selectedTab = selectedTab,
        canReportDeparture = canReportDeparture
    )
}
