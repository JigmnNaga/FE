package com.jigmnnaga.presentation.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatConversationContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsSystemNoticeSenderTimeAndOwnVersusOtherMessages() {
        composeRule.setContent {
            JigmnnagaTheme {
                ChatConversationContent(
                    state = ChatConversationUiState(
                        systemNotice = "채팅방이 열렸어요",
                        messages = listOf(
                            ChatMessageUiState("mine", "내 메시지", isMine = true, timeLabel = "오후 1:22"),
                            ChatMessageUiState("other", "상대 메시지", isMine = false, senderName = "태희", timeLabel = "오후 1:24")
                        )
                    ),
                    onMessageChange = {}, onSendMessage = {}
                )
            }
        }

        composeRule.onNodeWithTag("chat-system-notice").assertExists()
        composeRule.onNodeWithTag("chat-message-mine").assertExists()
        composeRule.onNodeWithTag("chat-message-other").assertExists()
        composeRule.onNodeWithText("태희").assertExists()
        composeRule.onNodeWithText("오후 1:24").assertExists()
    }

    @Test
    fun longMessageWrapsAndListCanScrollToOlderOrNewerItems() {
        val messages = (0..24).map { index ->
            ChatMessageUiState(
                id = "message-$index",
                body = if (index == 24) "긴 메시지입니다. 화면이 좁아도 말줄임 없이 여러 줄로 읽을 수 있도록 문장을 이어서 표시합니다." else "메시지 $index",
                isMine = index % 2 == 0,
                senderName = if (index % 2 == 0) null else "참여자 $index",
                timeLabel = "오후 1:$index"
            )
        }
        composeRule.setContent {
            JigmnnagaTheme {
                Box(Modifier.width(320.dp)) {
                    ChatConversationContent(
                        state = ChatConversationUiState(messages = messages),
                        onMessageChange = {}, onSendMessage = {}
                    )
                }
            }
        }

        val list = composeRule.onNodeWithTag("chat-message-list")
        list.performScrollToNode(hasText("메시지 23"))
        composeRule.onNodeWithText("메시지 23").assertExists()
        list.performScrollToNode(
            hasText("긴 메시지입니다. 화면이 좁아도 말줄임 없이 여러 줄로 읽을 수 있도록 문장을 이어서 표시합니다.")
        )
        composeRule.onNodeWithText("긴 메시지입니다. 화면이 좁아도 말줄임 없이 여러 줄로 읽을 수 있도록 문장을 이어서 표시합니다.")
            .assertExists()
    }

    @Test
    fun inputValueAndSendAreControlledByCaller() {
        val draft = mutableStateOf("")
        var sends = 0
        composeRule.setContent {
            JigmnnagaTheme {
                ChatConversationContent(
                    state = ChatConversationUiState(draftMessage = draft.value),
                    onMessageChange = { draft.value = it },
                    onSendMessage = { sends++ }
                )
            }
        }

        composeRule.onNodeWithTag("chat-conversation-input").assertExists()
        composeRule.onNode(hasSetTextAction()).performTextInput("안녕하세요")
        composeRule.runOnIdle { assertEquals("안녕하세요", draft.value) }
        composeRule.onNodeWithContentDescription("메시지 전송").performClick()
        composeRule.runOnIdle { assertEquals(1, sends) }
    }

    @Test
    fun emptyConversationDoesNotInventAnEmptyStateMessage() {
        composeRule.setContent {
            JigmnnagaTheme {
                ChatConversationContent(
                    state = ChatConversationUiState(),
                    onMessageChange = {}, onSendMessage = {}
                )
            }
        }

        composeRule.onNodeWithTag("chat-message-list").assertExists()
        composeRule.onNodeWithTag("chat-system-notice").assertDoesNotExist()
    }

    @Test
    fun supports320DpAndFontScalesFromOneToOnePointFive() {
        val fontScale = mutableStateOf(1f)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.value)) {
                JigmnnagaTheme {
                    Box(Modifier.width(320.dp)) {
                        ChatConversationContent(
                            state = ChatConversationUiState(
                                messages = listOf(
                                    ChatMessageUiState(
                                        id = "long",
                                        body = "긴 메시지 내용도 글자 크기가 커지면 줄 바꿈하여 표시할 수 있는지 확인합니다.",
                                        isMine = false,
                                        senderName = "아주 긴 참여자 이름",
                                        timeLabel = "오후 1:24"
                                    )
                                )
                            ),
                            onMessageChange = {}, onSendMessage = {}
                        )
                    }
                }
            }
        }

        listOf(1f, 1.3f, 1.5f).forEach { scale ->
            composeRule.runOnIdle { fontScale.value = scale }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("chat-conversation-content").assertExists()
            composeRule.onNodeWithText("긴 메시지 내용도 글자 크기가 커지면 줄 바꿈하여 표시할 수 있는지 확인합니다.")
                .assertExists()
        }
    }
}
