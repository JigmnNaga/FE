package com.jigmnnaga.presentation.appointment.manage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import com.jigmnnaga.presentation.appointment.create.AppointmentCreateUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppointmentManageScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun state(
        confirmedDateTime: String? = null,
        confirmedPlace: String? = null,
        isConfirmed: Boolean = false,
        inviteUrl: String? = "jigeumnaga.app/invite/study1017"
    ) = AppointmentManageUiState(
        saved = AppointmentCreateUiState(appointmentName = "중간고사 카공 모임"),
        form = AppointmentCreateUiState(appointmentName = "중간고사 카공 모임"),
        members = listOf(
            ManageMember(1L, "나", true, scheduleSubmitted = true),
            ManageMember(2L, "태희", false, scheduleSubmitted = true),
            ManageMember(3L, "하은", false, scheduleSubmitted = false)
        ),
        confirmedDateTime = confirmedDateTime,
        confirmedPlace = confirmedPlace,
        isConfirmed = isConfirmed,
        inviteUrl = inviteUrl
    )

    private fun setScreen(
        state: AppointmentManageUiState,
        onOpenConfirmation: () -> Unit = {},
        onCopyInviteLink: () -> Unit = {},
        onShareInviteLink: () -> Unit = {}
    ) {
        composeRule.setContent {
            JigmnnagaTheme {
                AppointmentManageScreen(
                    state = state,
                    onBack = {},
                    onOpenStep = {},
                    onFormChange = {},
                    onSave = {},
                    onRequestConfirm = {},
                    onDismissConfirm = {},
                    onConfirm = {},
                    onSendReminders = {},
                    onRequestKick = {},
                    onDismissKick = {},
                    onConfirmKick = {},
                    onOpenConfirmation = onOpenConfirmation,
                    onCopyInviteLink = onCopyInviteLink,
                    onShareInviteLink = onShareInviteLink
                )
            }
        }
    }

    @Test
    fun scheduleSectionShowsUndecidedBeforeConfirmation() {
        setScreen(state())

        composeRule.onNodeWithText("일정 조율").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("appointment-manage-confirm-date").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("appointment-manage-confirm-place").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun scheduleRowsOpenConfirmation() {
        var opened = 0
        setScreen(state(), onOpenConfirmation = { opened++ })

        composeRule.onNodeWithTag("appointment-manage-confirm-date").performScrollTo().performClick()
        composeRule.onNodeWithTag("appointment-manage-confirm-place").performScrollTo().performClick()

        assertEquals(2, opened)
    }

    @Test
    fun scheduleSectionShowsConfirmedValues() {
        setScreen(state(confirmedDateTime = "11월 20일 금요일 오후 7:00", confirmedPlace = "강남역 카공카페", isConfirmed = true))

        composeRule.onNodeWithText("11월 20일 금요일 오후 7:00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("강남역 카공카페").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun responseCardShowsCountsAndMemberGroups() {
        setScreen(state())

        composeRule.onNodeWithTag("appointment-manage-response-card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2/3명").assertIsDisplayed()
        composeRule.onNodeWithText("선택 완료 · 2명").assertIsDisplayed()
        composeRule.onNodeWithText("미완료 · 1명").assertIsDisplayed()
        composeRule.onNodeWithText("조율 중 · 저장된 응답만 시간표에 반영돼요.").assertIsDisplayed()
    }

    @Test
    fun reminderRowShowsPendingCountBeforeConfirmation() {
        setScreen(state())

        composeRule.onNodeWithTag("appointment-manage-reminder").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1명").assertIsDisplayed()
    }

    @Test
    fun hostOnlyBadgesAndReminderTargetAreShown() {
        setScreen(state())

        composeRule.onNodeWithText("약속 정보").assertIsDisplayed()
        composeRule.onNodeWithText("하은님에게 참석 응답 요청").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun inviteLinkAndActionsAreShownAndClickable() {
        var copied = 0
        var shared = 0
        setScreen(state(), onCopyInviteLink = { copied++ }, onShareInviteLink = { shared++ })

        composeRule.onNodeWithTag("appointment-manage-invite-link").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("jigeumnaga.app/invite/study1017").assertIsDisplayed()
        composeRule.onNodeWithTag("appointment-manage-copy-link").performScrollTo().performClick()
        composeRule.onNodeWithTag("appointment-manage-share-link").performScrollTo().performClick()

        assertEquals(1, copied)
        assertEquals(1, shared)
    }

    @Test
    fun responseCardCollapsesWhenHeaderIsTapped() {
        setScreen(state())

        composeRule.onNodeWithTag("appointment-manage-response-toggle").performScrollTo().performClick()

        composeRule.onNodeWithText("미완료 · 1명").assertDoesNotExist()
        composeRule.onNodeWithTag("appointment-manage-invite-link").assertDoesNotExist()
    }
}
