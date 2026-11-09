package com.jigmnnaga.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jigmnnaga.core.designsystem.component.ChatInputBar
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.core.designsystem.theme.JigmnnagaTheme

/**
 * Reusable conversation area for CHA-001-01. The caller supplies messages and owns navigation,
 * IME/system insets, data loading, and send behavior. This composable never simulates a send result.
 */
@Composable
fun ChatConversationContent(
    state: ChatConversationUiState,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .testTag("chat-conversation-content")
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("chat-message-list"),
            contentPadding = PaddingValues(
                horizontal = AppDimens.Spacing20,
                vertical = AppDimens.Spacing12
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
        ) {
            state.systemNotice?.takeIf(String::isNotBlank)?.let { notice ->
                item(key = "conversation-system-notice") {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = notice,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AppColors.Monotone.Gray100)
                                .padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing8)
                                .testTag("chat-system-notice"),
                            textAlign = TextAlign.Center,
                            color = AppColors.Monotone.Gray600,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            items(state.messages, key = ChatMessageUiState::id) { message ->
                ChatMessageRow(message)
            }
        }

        ChatInputBar(
            value = state.draftMessage,
            onValueChange = onMessageChange,
            onSendClick = onSendMessage,
            modifier = Modifier.testTag("chat-conversation-input")
        )
    }
}

@Composable
private fun ChatMessageRow(message: ChatMessageUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat-message-${message.id}"),
        horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start
    ) {
        if (!message.isMine && !message.senderName.isNullOrBlank()) {
            Text(
                text = message.senderName,
                modifier = Modifier.padding(start = AppDimens.Spacing8, bottom = AppDimens.Spacing4),
                color = AppColors.Monotone.Gray600,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Surface(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .semantics(mergeDescendants = true) {},
            color = if (message.isMine) AppColors.MainGreen.Green700 else AppColors.Monotone.White,
            shape = RoundedCornerShape(AppDimens.Spacing16)
        ) {
            Text(
                text = message.body,
                modifier = Modifier.padding(
                    horizontal = AppDimens.Spacing12,
                    vertical = AppDimens.Spacing8
                ),
                color = if (message.isMine) AppColors.Monotone.White else AppColors.Monotone.SoftBlack,
                style = MaterialTheme.typography.bodySmall
            )
        }

        message.timeLabel?.takeIf(String::isNotBlank)?.let { time ->
            Text(
                text = time,
                modifier = Modifier.padding(horizontal = AppDimens.Spacing8, vertical = AppDimens.Spacing4),
                color = AppColors.Monotone.Gray500,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 760)
@Composable
private fun ChatConversationContentPreview() {
    JigmnnagaTheme {
        ChatConversationContent(
            state = ChatConversationUiState(
                systemNotice = "채팅방이 열렸어요",
                messages = listOf(
                    ChatMessageUiState("mine-1", "다들 이번 주 토요일 어때요?", isMine = true, timeLabel = "오후 1:22"),
                    ChatMessageUiState("other-1", "저는 오후 6시 이후면 괜찮아요!", isMine = false, senderName = "태희", timeLabel = "오후 1:24"),
                    ChatMessageUiState("other-2", "저도 토요일 저녁 좋아요", isMine = false, senderName = "하은", timeLabel = "오후 1:25")
                )
            ),
            onMessageChange = {},
            onSendMessage = {}
        )
    }
}
