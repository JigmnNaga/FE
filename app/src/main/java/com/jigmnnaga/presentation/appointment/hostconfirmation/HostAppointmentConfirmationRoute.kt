package com.jigmnnaga.presentation.appointment.hostconfirmation

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
 * 방장 최종 확정 화면과 [HostConfirmationViewModel]을 연결한다.
 * 화면을 닫는 동작은 호출하는 쪽이 [onExit]으로, 로그인 이동은 [onLoginRequired]로 넘기고,
 * 확정 실패 안내는 스낵바로 보여 준다.
 */
@Composable
fun HostAppointmentConfirmationRoute(
    viewModel: HostConfirmationViewModel,
    onExit: () -> Unit,
    onLoginRequired: (resumeAfterLogin: () -> Unit) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadTimeCandidates()
    }

    LaunchedEffect(notice) {
        val current = notice ?: return@LaunchedEffect
        val messageResource = when (current) {
            HostConfirmationNotice.CONFIRM_API_NOT_CONNECTED -> R.string.host_confirmation_api_unavailable
            HostConfirmationNotice.LOGIN_REQUIRED -> {
                viewModel.clearNotice()
                onLoginRequired(viewModel::loadTimeCandidates)
                null
            }
            HostConfirmationNotice.NOT_HOST -> R.string.host_confirmation_not_host
            HostConfirmationNotice.APPOINTMENT_CANCELED -> R.string.host_confirmation_canceled
            HostConfirmationNotice.INVALID_SELECTION -> R.string.host_confirmation_invalid_selection
            HostConfirmationNotice.FAILURE_RETRYABLE -> R.string.host_confirmation_failure_retry
            HostConfirmationNotice.FAILURE -> R.string.host_confirmation_failure
        }
        if (messageResource != null) {
            snackbarHostState.showSnackbar(context.getString(messageResource))
            viewModel.clearNotice()
        }
    }

    // 선택 단계에서 뒤로 가면 확인 단계로 돌아가고, 확인 단계에서는 시스템 기본 동작(화면 닫기)을 따른다.
    BackHandler(enabled = state.step != HostConfirmationStep.REVIEW && state.step != HostConfirmationStep.COMPLETE) {
        viewModel.onBack()
    }

    val actions = remember(viewModel, onExit) {
        HostConfirmationActions(
            onBack = { if (!viewModel.onBack()) onExit() },
            onEditDateTime = viewModel::onEditDateTime,
            onEditPlace = viewModel::onEditPlace,
            onSelectDateTime = viewModel::onSelectDateTime,
            onSelectPlace = viewModel::onSelectPlace,
            onOpenDetailLocation = viewModel::onOpenDetailLocation,
            onSelectDetailLocation = viewModel::onSelectDetailLocation,
            onChooseDetailLater = viewModel::onChooseDetailLater,
            onConfirm = viewModel::onConfirm,
            onComplete = onExit,
            onDetailSearchQueryChange = viewModel::onDetailSearchQueryChange
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        HostAppointmentConfirmationScreen(state = state, actions = actions)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 80.dp)
        )
    }
}
