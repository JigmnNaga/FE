package com.jigmnnaga.presentation.appointment.manage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
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
 * 약속 관리 화면과 [AppointmentManageViewModel]을 연결한다.
 *
 * @param onExit 관리 목록에서 뒤로 갈 때 호출한다.
 * @param onDeleted 약속방을 삭제한 뒤 호출한다. 삭제된 약속방으로 돌아갈 수 없으므로 홈 등으로 보낸다.
 * @param onLoginRequired 로그인이 필요할 때 호출한다. 로그인에 성공하면 실행할 일을 인자로 넘긴다.
 */
@Composable
fun AppointmentManageRoute(
    viewModel: AppointmentManageViewModel,
    onExit: () -> Unit,
    onDeleted: () -> Unit,
    onLoginRequired: (resume: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = state.step != AppointmentManageStep.MENU) { viewModel.back() }

    LaunchedEffect(state.finished) {
        if (state.finished == AppointmentManageFinish.DELETED) onDeleted()
    }

    LaunchedEffect(notice) {
        val current = notice ?: return@LaunchedEffect
        if (current == AppointmentManageNotice.LOGIN_REQUIRED) {
            viewModel.clearNotice()
            onLoginRequired(viewModel::resumeAfterLogin)
            return@LaunchedEffect
        }
        val messageResource = when (current) {
            AppointmentManageNotice.NOT_HOST -> R.string.appointment_manage_not_host
            AppointmentManageNotice.APPOINTMENT_CANCELED -> R.string.appointment_manage_already_canceled
            AppointmentManageNotice.INVALID_DATES -> R.string.appointment_manage_invalid_dates
            AppointmentManageNotice.INVALID_DEADLINE -> R.string.appointment_manage_invalid_deadline
            AppointmentManageNotice.DATES_BEFORE_DEADLINE -> R.string.appointment_manage_dates_before_deadline
            AppointmentManageNotice.INVALID_LOCATIONS -> R.string.appointment_manage_invalid_locations
            AppointmentManageNotice.INVALID_INPUT -> R.string.appointment_manage_invalid_input
            AppointmentManageNotice.FAILED -> R.string.appointment_manage_failed
            AppointmentManageNotice.FAILED_RETRY -> R.string.appointment_manage_failed_retry
            AppointmentManageNotice.LOCATION_SEARCH_FAILED -> R.string.appointment_location_search_failed
            AppointmentManageNotice.SAVED -> R.string.appointment_manage_saved
            AppointmentManageNotice.CANCELED_DONE -> R.string.appointment_manage_canceled_done
            AppointmentManageNotice.KICKED -> R.string.appointment_manage_kicked
            AppointmentManageNotice.REMINDER_SENT -> R.string.appointment_manage_reminder_sent
            AppointmentManageNotice.REMINDER_NONE -> R.string.appointment_manage_reminder_none
            AppointmentManageNotice.LOGIN_REQUIRED -> return@LaunchedEffect
        }
        val message = if (current == AppointmentManageNotice.REMINDER_SENT) {
            context.getString(messageResource, state.lastReminderSentCount ?: 0)
        } else {
            context.getString(messageResource)
        }
        snackbarHostState.showSnackbar(message)
        viewModel.clearNotice()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AppointmentManageScreen(
            state = state,
            onBack = { if (!viewModel.back()) onExit() },
            onOpenStep = viewModel::openStep,
            onFormChange = viewModel::onFormChange,
            onSave = viewModel::save,
            onRequestConfirm = viewModel::requestConfirm,
            onDismissConfirm = viewModel::dismissConfirm,
            onConfirm = viewModel::confirmAction,
            onSendReminders = viewModel::sendReminders,
            onRequestKick = viewModel::requestKick,
            onDismissKick = viewModel::dismissKick,
            onConfirmKick = viewModel::confirmKick
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
