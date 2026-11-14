package com.jigmnnaga.presentation.appointment.host

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jigmnnaga.R
import com.jigmnnaga.presentation.chat.AppointmentChatViewModel
import com.jigmnnaga.presentation.chat.ChatReadingEffect
import com.jigmnnaga.presentation.chat.NoChat
import com.jigmnnaga.presentation.chat.toHostRoomMessage
import com.jigmnnaga.presentation.chat.toMessageResource

/**
 * 방장 약속방 화면과 [HostAppointmentRoomViewModel]을 연결한다.
 * 화면 이동은 호출하는 쪽이 콜백으로 넘기고, 서버 미연동 안내만 스낵바로 보여 준다.
 */
@Composable
fun HostAppointmentRoomRoute(
    viewModel: HostAppointmentRoomViewModel,
    onBackClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onPlaceClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    chatViewModel: AppointmentChatViewModel? = null
) {
    val roomState by viewModel.uiState.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val chatState by (chatViewModel?.uiState ?: NoChat.state).collectAsState()
    val chatNotice by (chatViewModel?.notice ?: NoChat.notice).collectAsState()
    val state = if (chatViewModel == null) {
        roomState
    } else {
        roomState.copy(messages = chatState.messages.map { it.toHostRoomMessage() })
    }
    ChatReadingEffect(chatViewModel, chatTabSelected = state.selectedTab == HostAppointmentRoomTab.CHAT)
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notice) {
        val messageResource = when (notice) {
            HostAppointmentRoomNotice.CHAT_API_NOT_CONNECTED -> R.string.appointment_room_chat_unavailable
            null -> null
        }
        if (messageResource != null) {
            snackbarHostState.showSnackbar(context.getString(messageResource))
            viewModel.clearNotice()
        }
    }

    LaunchedEffect(chatNotice) {
        val messageResource = chatNotice?.toMessageResource()
        if (messageResource != null) {
            snackbarHostState.showSnackbar(context.getString(messageResource))
            chatViewModel?.clearNotice()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HostAppointmentRoomScreen(
            state = state,
            onBackClick = onBackClick,
            onCalendarClick = onCalendarClick,
            onPlaceClick = onPlaceClick,
            onMoreClick = onMoreClick,
            onTabSelected = viewModel::onTabSelected,
            onSummaryExpandedChange = viewModel::onSummaryExpandedChange,
            onMessageChange = viewModel::onMessageChange,
            onSendMessage = {
                if (chatViewModel == null) {
                    viewModel.onSendMessage()
                } else if (chatViewModel.sendMessage(state.draftMessage)) {
                    // 보낸 메시지는 서버에서 되돌아와 목록에 붙는다. 보냈을 때만 입력창을 비운다.
                    viewModel.onMessageChange("")
                }
            },
            canLoadMoreMessages = chatViewModel != null && chatState.hasMore && !chatState.isLoadingMore,
            onLoadMoreMessages = { chatViewModel?.loadMore() }
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 80.dp)
        )
    }
}
