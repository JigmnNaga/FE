package com.jigmnnaga

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.BottomNavigationBar
import com.jigmnnaga.core.designsystem.component.BottomNavigationTab
import com.jigmnnaga.core.designsystem.component.ButtonVariant
import com.jigmnnaga.core.designsystem.component.CircleButton
import com.jigmnnaga.core.designsystem.component.ChatInputBar
import com.jigmnnaga.core.designsystem.component.MainHeader
import com.jigmnnaga.core.designsystem.component.MeetingHeader
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DesignSystemAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun chatInputBlocksEmptyAndWhitespaceForImeAndSendButton() {
        var sendCount = 0
        val message = mutableStateOf("")
        composeRule.setContent {
            JigmnnagaTheme {
                ChatInputBar(value = message.value, onValueChange = { message.value = it }, onSendClick = { sendCount++ })
            }
        }

        val input = composeRule.onNode(hasSetTextAction())
        input.performClick()
        input.performImeAction()
        composeRule.onNodeWithContentDescription("메시지 전송").assertIsNotEnabled()
        composeRule.runOnIdle { assertEquals(0, sendCount) }

        input.performTextInput("   ")
        composeRule.onNodeWithContentDescription("메시지 전송").assertIsNotEnabled()
        input.performImeAction()
        composeRule.runOnIdle { assertEquals(0, sendCount) }
        composeRule.onNodeWithContentDescription("메시지 전송").performClick()
        composeRule.runOnIdle { assertEquals(0, sendCount) }
    }

    @Test
    fun chatInputImeSendUsesCallbackAndKeepsInputFocused() {
        var sendCount = 0
        val message = mutableStateOf("")
        composeRule.setContent {
            JigmnnagaTheme {
                ChatInputBar(value = message.value, onValueChange = { message.value = it }, onSendClick = { sendCount++ })
            }
        }
        val input = composeRule.onNode(hasSetTextAction())
        input.performClick()
        input.performTextInput("안녕하세요")
        input.performImeAction()
        composeRule.runOnIdle { assertEquals(1, sendCount) }
        input.assertIsFocused()
    }

    @Test
    fun chatInputSendButtonUsesCallbackForNonblankMessage() {
        var sendCount = 0
        composeRule.setContent {
            JigmnnagaTheme {
                ChatInputBar(value = "보낼 메시지", onValueChange = {}, onSendClick = { sendCount++ })
            }
        }
        composeRule.onNodeWithContentDescription("메시지 전송").performClick()
        composeRule.runOnIdle { assertEquals(1, sendCount) }
    }

    @Test
    fun mainHeaderUsesFallbackForNullEmptyAndWhitespaceProfileNames() {
        composeRule.setContent {
            JigmnnagaTheme {
                Column {
                    MainHeader(onNotificationClick = {}, onProfileClick = {}, profileLabel = null)
                    MainHeader(onNotificationClick = {}, onProfileClick = {}, profileLabel = "")
                    MainHeader(onNotificationClick = {}, onProfileClick = {}, profileLabel = "   ")
                }
            }
        }
        composeRule.onAllNodesWithContentDescription("프로필").assertCountEquals(3)
        composeRule.onAllNodesWithContentDescription("프로필")[0].assertHasClickAction()
    }

    @Test
    fun mainHeaderShowsNormalProfileName() {
        composeRule.setContent {
            JigmnnagaTheme {
                MainHeader(onNotificationClick = {}, onProfileClick = {}, profileLabel = "민지")
            }
        }
        composeRule.onNodeWithContentDescription("프로필").assertHasClickAction()
        composeRule.onNodeWithText("민지").assertExists()
    }

    @Test
    fun meetingHeaderMoreButtonHasAccessibleDescription() {
        composeRule.setContent {
            JigmnnagaTheme {
                MeetingHeader(
                    onBackClick = {}, onCalendarClick = {}, onPlaceClick = {}, onMoreClick = {},
                    title = "모임", subtitle = "오늘"
                )
            }
        }
        composeRule.onNodeWithContentDescription("더보기").assertHasClickAction()
    }

    @Test
    fun disabledButtonDoesNotInvokeCallback() {
        var clickCount = 0
        composeRule.setContent {
            JigmnnagaTheme {
                AppButton(text = "계속", variant = ButtonVariant.PRIMARY, enabled = false, onClick = { clickCount++ })
            }
        }
        composeRule.onNodeWithText("계속").assertIsNotEnabled().performClick()
        composeRule.runOnIdle { assertEquals(0, clickCount) }
    }

    @Test
    fun bottomNavigationSelectionAndCallbacksFollowClicks() {
        var homeClicks = 0
        var scheduleClicks = 0
        composeRule.setContent {
            JigmnnagaTheme {
                val selectedState = androidx.compose.runtime.remember { mutableStateOf(BottomNavigationTab.HOME) }
                BottomNavigationBar(
                    selectedTab = selectedState.value,
                    onHomeClick = { homeClicks++; selectedState.value = BottomNavigationTab.HOME },
                    onAddClick = {},
                    onScheduleClick = { scheduleClicks++; selectedState.value = BottomNavigationTab.SCHEDULE }
                )
            }
        }
        composeRule.onNodeWithText("홈").assertIsSelected().performClick()
        composeRule.runOnIdle { assertEquals(1, homeClicks) }
        composeRule.onNodeWithText("일정").assertHasClickAction().performClick().assertIsSelected()
        composeRule.runOnIdle { assertEquals(1, scheduleClicks) }
        composeRule.onNodeWithText("홈").assertIsNotSelected()
    }

    @Test
    fun compactCircleButtonsExposeNonOverlappingTouchTargets() {
        var firstClicks = 0
        var secondClicks = 0
        composeRule.setContent {
            JigmnnagaTheme {
                Row {
                    CircleButton(contentDescription = "첫 번째", onClick = { firstClicks++ }, width = 28.dp, height = 28.dp, text = "1")
                    Spacer(Modifier.width(20.dp))
                    CircleButton(contentDescription = "두 번째", onClick = { secondClicks++ }, width = 28.dp, height = 28.dp, text = "2")
                }
            }
        }
        val first = composeRule.onNodeWithContentDescription("첫 번째")
        val second = composeRule.onNodeWithContentDescription("두 번째")
        first.assertWidthIsAtLeast(48.dp)
        second.assertWidthIsAtLeast(48.dp)

        val firstTouchBounds = first.fetchSemanticsNode().boundsInRoot
        val secondTouchBounds = second.fetchSemanticsNode().boundsInRoot
        val separated = firstTouchBounds.right <= secondTouchBounds.left ||
            secondTouchBounds.right <= firstTouchBounds.left ||
            firstTouchBounds.bottom <= secondTouchBounds.top ||
            secondTouchBounds.bottom <= firstTouchBounds.top
        assertTrue("Adjacent expanded touch bounds overlap: $firstTouchBounds vs $secondTouchBounds", separated)

        composeRule.onRoot().performTouchInput {
            click(Offset(firstTouchBounds.right - 1f, firstTouchBounds.center.y))
        }
        composeRule.runOnIdle { assertEquals(1, firstClicks); assertEquals(0, secondClicks) }
        composeRule.onRoot().performTouchInput {
            click(Offset(secondTouchBounds.left + 1f, secondTouchBounds.center.y))
        }
        composeRule.runOnIdle { assertEquals(1, firstClicks); assertEquals(1, secondClicks) }
    }

    @Test
    fun bottomNavigationTabsHaveSeparateTouchTargetsAndCallbacks() {
        var homeClicks = 0
        var scheduleClicks = 0
        composeRule.setContent {
            JigmnnagaTheme {
                BottomNavigationBar(
                    selectedTab = BottomNavigationTab.NONE,
                    onHomeClick = { homeClicks++ }, onAddClick = {}, onScheduleClick = { scheduleClicks++ }
                )
            }
        }
        composeRule.onNodeWithText("홈").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("일정").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("홈").performClick()
        composeRule.runOnIdle { assertEquals(1, homeClicks); assertEquals(0, scheduleClicks) }
        composeRule.onNodeWithText("일정").performClick()
        composeRule.runOnIdle { assertEquals(1, homeClicks); assertEquals(1, scheduleClicks) }
    }

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(BuildConfig.APPLICATION_ID, appContext.packageName)
    }
}
