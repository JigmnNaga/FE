package com.jigmnnaga.presentation.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationUiStateTest {

    private val invite = InviteNotificationItem(
        id = 1,
        inviterName = "태양",
        metaLabel = "방금 · 4명",
        appointmentTitle = "금요일 저녁 모임",
        status = InviteNotificationStatus.PENDING
    )

    private val activity = ActivityNotificationItem(
        id = 1,
        type = ActivityNotificationType.ARRIVED,
        message = "지우가 도착했어요",
        subtitle = "금요일 저녁 모임 · 32분 전"
    )

    @Test
    fun `초기 상태는 초대 탭이고 알림이 없다`() {
        val state = NotificationUiState()

        assertEquals(NotificationTab.INVITE, state.selectedTab)
        assertTrue(state.isCurrentTabEmpty)
    }

    @Test
    fun `초대 탭은 초대 알림이 있으면 비어 있지 않다`() {
        val state = NotificationUiState(invites = listOf(invite))

        assertFalse(state.isCurrentTabEmpty)
    }

    @Test
    fun `활동 알림만 있으면 초대 탭은 비어 있고 활동 탭은 비어 있지 않다`() {
        val inviteTab = NotificationUiState(
            selectedTab = NotificationTab.INVITE,
            activities = listOf(activity)
        )
        val activityTab = inviteTab.copy(selectedTab = NotificationTab.ACTIVITY)

        assertTrue(inviteTab.isCurrentTabEmpty)
        assertFalse(activityTab.isCurrentTabEmpty)
    }

    @Test
    fun `초대 알림만 있으면 활동 탭은 비어 있다`() {
        val state = NotificationUiState(
            selectedTab = NotificationTab.ACTIVITY,
            invites = listOf(invite)
        )

        assertTrue(state.isCurrentTabEmpty)
    }
}
