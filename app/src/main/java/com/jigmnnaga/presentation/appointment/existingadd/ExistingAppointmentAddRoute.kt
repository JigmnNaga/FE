package com.jigmnnaga.presentation.appointment.existingadd

import androidx.activity.compose.BackHandler
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
 * 기존 약속 추가 화면과 [ExistingAppointmentAddViewModel]을 연결한다.
 * 화면 이동은 호출하는 쪽이 넘기고, 안내만 스낵바로 보여 준다.
 * "약속 요청하기"로 고른 참여자는 [onRequestMembers]로 넘긴다.
 */
@Composable
fun ExistingAppointmentAddRoute(
    viewModel: ExistingAppointmentAddViewModel,
    onExit: () -> Unit,
    onCalendarClick: () -> Unit,
    onPlaceClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRequestMembers: (PreviousMembersRequest) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val requestedMembers by viewModel.requestedMembers.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notice) {
        val messageResource = when (notice) {
            ExistingAppointmentAddNotice.REQUEST_API_NOT_CONNECTED ->
                R.string.existing_add_request_api_unavailable
            ExistingAppointmentAddNotice.CHAT_NOT_CONNECTED ->
                R.string.existing_add_chat_unavailable
            ExistingAppointmentAddNotice.REQUEST_MEMBERS_INVALID ->
                R.string.existing_add_request_members_invalid
            null -> null
        }
        if (messageResource != null) {
            snackbarHostState.showSnackbar(context.getString(messageResource))
            viewModel.clearNotice()
        }
    }

    LaunchedEffect(requestedMembers) {
        requestedMembers?.let {
            onRequestMembers(it)
            viewModel.consumeRequestedMembers()
        }
    }

    val handleBack = {
        if (!viewModel.onBackClick()) onExit()
    }
    BackHandler(enabled = state.page == ExistingAppointmentAddPage.REQUEST_PARTICIPANTS) {
        viewModel.onBackClick()
    }

    Box(modifier = modifier.fillMaxSize()) {
        ExistingAppointmentAddScreen(
            state = state,
            onBackClick = handleBack,
            onCalendarClick = onCalendarClick,
            onPlaceClick = onPlaceClick,
            onMoreClick = onMoreClick,
            onChatTabClick = viewModel::onChatTabClick,
            onMapAndStatusTabClick = viewModel::onMapAndStatusTabClick,
            onMemberSelectionChange = viewModel::onMemberSelectionChange,
            onRescheduleClick = viewModel::onRescheduleClick,
            onRequestClick = viewModel::onRequestClick
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
