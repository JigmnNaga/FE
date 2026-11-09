package com.jigmnnaga

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.core.designsystem.component.BottomNavigationBar
import com.jigmnnaga.core.designsystem.component.BottomNavigationTab
import com.jigmnnaga.core.designsystem.component.ButtonVariant
import com.jigmnnaga.core.designsystem.component.ChatInputBar
import com.jigmnnaga.core.designsystem.component.MainHeader
import com.jigmnnaga.core.designsystem.component.MeetingHeader
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DesignSystemResponsiveLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sharedComponentsStayInside320DpAtAccessibleFontScales() {
        val longMeetingTitle = "주말 산행과 맛집 방문을 함께하는 긴 모임 이름"
        val fontScale = mutableFloatStateOf(1f)
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(baseDensity.density, fontScale.floatValue)
            ) {
                JigmnnagaTheme {
                    Column(
                        Modifier
                            .width(320.dp)
                            .testTag("responsive-root")
                    ) {
                        MainHeader(
                            onNotificationClick = {},
                            onProfileClick = {},
                            profileLabel = "민지",
                            modifier = Modifier.testTag("main-header")
                        )
                        MeetingHeader(
                            onBackClick = {},
                            onCalendarClick = {},
                            onPlaceClick = {},
                            onMoreClick = {},
                            title = longMeetingTitle,
                            subtitle = "참여자 12명 · 응답 대기 중",
                            modifier = Modifier.testTag("meeting-header")
                        )
                        BackHeader(
                            title = "매우 긴 화면 제목도 잘리지 않고 안전하게 배치",
                            onBackClick = {},
                            modifier = Modifier.testTag("back-header")
                        )
                        ChatInputBar(
                            value = "긴 입력 예시",
                            onValueChange = {},
                            onSendClick = {},
                            modifier = Modifier.testTag("chat-input")
                        )
                        AppButton(
                            text = "계속 진행하기",
                            variant = ButtonVariant.PRIMARY,
                            onClick = {},
                            modifier = Modifier.testTag("primary-button")
                        )
                        AppButton(
                            text = "시작하기",
                            variant = ButtonVariant.START,
                            onClick = {},
                            modifier = Modifier.testTag("start-button")
                        )
                        BottomNavigationBar(
                            selectedTab = BottomNavigationTab.HOME,
                            onHomeClick = {},
                            onAddClick = {},
                            onScheduleClick = {},
                            modifier = Modifier.testTag("bottom-navigation")
                        )
                    }
                }
            }
        }

        listOf(1f, 1.3f, 1.5f).forEach { scale ->
            composeRule.runOnIdle { fontScale.floatValue = scale }
            composeRule.waitForIdle()

            val root = bounds("responsive-root")
            listOf(
                "main-header",
                "meeting-header",
                "back-header",
                "chat-input",
                "primary-button",
                "start-button",
                "bottom-navigation"
            ).forEach { tag ->
                val child = bounds(tag)
                assertTrue(
                    "$tag exceeds the 320dp root at font scale $scale: $child vs $root",
                    child.left >= root.left && child.right <= root.right
                )
            }

            val meetingHeader = bounds("meeting-header")
            val back = composeRule
                .onAllNodesWithContentDescription("뒤로가기")[0]
                .fetchSemanticsNode()
                .boundsInRoot
            val titleNode = composeRule.onNodeWithText(longMeetingTitle)
            val title = titleNode.fetchSemanticsNode().boundsInRoot
            val calendar = composeRule.onNodeWithContentDescription("일정").fetchSemanticsNode().boundsInRoot
            val place = composeRule.onNodeWithContentDescription("장소").fetchSemanticsNode().boundsInRoot
            val more = composeRule.onNodeWithContentDescription("더보기").fetchSemanticsNode().boundsInRoot

            assertTrue(
                "Header controls must remain ordered and non-overlapping at font scale $scale",
                back.right <= title.left &&
                    title.right <= calendar.left &&
                    calendar.right <= place.left &&
                    place.right <= more.left
            )
            assertTrue(
                "Header must remain within its 320dp container at font scale $scale",
                back.left >= meetingHeader.left && more.right <= meetingHeader.right
            )
            val titleLayouts = mutableListOf<TextLayoutResult>()
            titleNode.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getResult ->
                assertTrue(getResult(titleLayouts))
            }
            assertTrue(
                "Long meeting title should use its one-line ellipsis policy at font scale $scale",
                titleLayouts.single().hasVisualOverflow
            )
            assertTrue(
                "Visible title text should be ellipsized inside its assigned width at font scale $scale",
                titleLayouts.single().getLineEnd(0, visibleEnd = true) < longMeetingTitle.length
            )

            composeRule.onNodeWithContentDescription("메시지 전송").assertExists()
            composeRule.onNodeWithText("계속 진행하기").assertExists()
            composeRule.onNodeWithTag("start-button-arrow", useUnmergedTree = true).assertExists()
            composeRule.onNodeWithText("홈").assertExists()
            composeRule.onNodeWithText("일정").assertExists()
        }
    }

    private fun bounds(tag: String) = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
}
