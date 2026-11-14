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
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val notice by viewModel.notice.collectAsState()
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
            onSendMessage = viewModel::onSendMessage
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
