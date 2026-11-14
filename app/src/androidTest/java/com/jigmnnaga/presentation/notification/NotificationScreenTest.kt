package com.jigmnnaga.presentation.notification

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NotificationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val invites = listOf(
        InviteNotificationItem(1, "태양", "방금 · 4명", "금요일 저녁 모임", InviteNotificationStatus.PENDING),
        InviteNotificationItem(2, "지우", "어제 · 6명", "스터디 정기 모임", InviteNotificationStatus.HANDLED)
    )

    private val activities = listOf(
        ActivityNotificationItem(
            id = 7,
            type = ActivityNotificationType.ARRIVED,
            message = "지우가 도착했어요",
            subtitle = "금요일 저녁 모임 · 32분 전"
        )
    )

    private fun show(
        state: NotificationUiState,
        onBackClick: () -> Unit = {},
        onTabSelect: (NotificationTab) -> Unit = {},
        onInviteConfirmClick: (Long) -> Unit = {},
        onActivityClick: (Long) -> Unit = {}
    ) {
        composeRule.setContent {
            JigmnnagaTheme {
                NotificationScreen(
                    state = state,
                    onBackClick = onBackClick,
                    onTabSelect = onTabSelect,
                    onInviteConfirmClick = onInviteConfirmClick,
                    onActivityClick = onActivityClick
                )
            }
        }
    }

    @Test
    fun inviteTabShowsPendingAndHandledCards() {
        show(NotificationUiState(invites = invites))

        composeRule.onNodeWithText("태양이(가) 초대했어요").assertIsDisplayed()
        composeRule.onNodeWithText("미확인").assertIsDisplayed()
        composeRule.onNodeWithTag("notification-invite-confirm-1").assertIsDisplayed()
        composeRule.onNodeWithText("이미 처리된 초대예요").assertIsDisplayed()
    }

    @Test
    fun confirmButtonForwardsInviteId() {
        var clickedId: Long? = null
        show(NotificationUiState(invites = invites), onInviteConfirmClick = { clickedId = it })

        composeRule.onNodeWithTag("notification-invite-confirm-1").performClick()

        assertEquals(1L, clickedId)
    }

    @Test
    fun tabClickForwardsSelectedTab() {
        var selected: NotificationTab? = null
        show(NotificationUiState(invites = invites), onTabSelect = { selected = it })

        composeRule.onNodeWithTag("notification-tab-activity").performClick()

        assertEquals(NotificationTab.ACTIVITY, selected)
    }

    @Test
    fun activityTabShowsItemsAndForwardsClick() {
        var clickedId: Long? = null
        show(
            NotificationUiState(selectedTab = NotificationTab.ACTIVITY, activities = activities),
            onActivityClick = { clickedId = it }
        )

        composeRule.onNodeWithText("지우가 도착했어요").assertIsDisplayed()
        composeRule.onNodeWithTag("notification-activity-7").performClick()

        assertEquals(7L, clickedId)
    }

    @Test
    fun emptyTabShowsEmptyMessage() {
        show(NotificationUiState())

        composeRule.onNodeWithTag("notification-empty").assertIsDisplayed()
        composeRule.onNodeWithText("아직 알림이 없어요").assertIsDisplayed()
    }

    @Test
    fun backButtonInvokesCallback() {
        var backClicked = false
        show(NotificationUiState(), onBackClick = { backClicked = true })

        composeRule.onNodeWithText("알림").assertIsDisplayed()
        composeRule.onNodeWithTag("notification-screen").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()

        assertEquals(true, backClicked)
    }
}
