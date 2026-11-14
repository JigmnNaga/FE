package com.jigmnnaga.presentation.appointment.finalconfirmation

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
 * 약속 확정 확인 화면과 [AppointmentFinalConfirmationViewModel]을 연결한다.
 * 화면을 닫는 동작은 호출하는 쪽이 [onExit]으로, 로그인 이동은 [onLoginRequired]로 넘기고,
 * 참석 응답이 저장되면 화면을 닫고 실패 안내는 스낵바로 보여 준다.
 */
@Composable
fun AppointmentFinalConfirmationRoute(
    viewModel: AppointmentFinalConfirmationViewModel,
    onExit: () -> Unit,
    onLoginRequired: (resumeAfterLogin: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notice) {
        val current = notice ?: return@LaunchedEffect
        val messageResource = when (current) {
            AppointmentFinalConfirmationNotice.SUBMITTED -> {
                viewModel.clearNotice()
                onExit()
                null
            }
            AppointmentFinalConfirmationNotice.LOGIN_REQUIRED -> {
                viewModel.clearNotice()
                onLoginRequired(viewModel::onConfirm)
                null
            }
            AppointmentFinalConfirmationNotice.NOT_PARTICIPANT -> R.string.final_confirmation_not_participant
            AppointmentFinalConfirmationNotice.APPOINTMENT_CANCELED -> R.string.final_confirmation_canceled
            AppointmentFinalConfirmationNotice.NOT_CONFIRMED -> R.string.final_confirmation_not_confirmed
            AppointmentFinalConfirmationNotice.TIME_PASSED -> R.string.final_confirmation_time_passed
            AppointmentFinalConfirmationNotice.INVALID_ANSWER -> R.string.final_confirmation_failure
            AppointmentFinalConfirmationNotice.FAILURE_RETRYABLE -> R.string.final_confirmation_failure_retry
            AppointmentFinalConfirmationNotice.FAILURE -> R.string.final_confirmation_failure
        }
        if (messageResource != null) {
            snackbarHostState.showSnackbar(context.getString(messageResource))
            viewModel.clearNotice()
        }
    }

    val actions = remember(viewModel, onExit) {
        AppointmentFinalConfirmationActions(
            onBack = onExit,
            onAttendanceChoiceChange = viewModel::onAttendanceChoiceChange,
            onConfirm = viewModel::onConfirm
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        AppointmentFinalConfirmationScreen(state = state, actions = actions)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 80.dp)
        )
    }
}
