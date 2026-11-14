package com.jigmnnaga.presentation.appointment.participant

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors

/**
 * 참여자 약속방 관리 화면(APT-005-02·06)과 [ParticipantAppointmentManagementViewModel]을 연결한다.
 *
 * @param onEditAvailabilityClick 희망 날짜·시간 수정 화면으로 이동한다.
 * @param onLeft 약속방을 나간 뒤 호출한다. 나간 약속방으로 돌아갈 수 없으므로 홈 등으로 보낸다.
 * @param onLoginRequired 로그인이 필요할 때 호출한다. 로그인에 성공하면 실행할 일을 인자로 넘긴다.
 */
@Composable
fun ParticipantAppointmentManagementRoute(
    viewModel: ParticipantAppointmentManagementViewModel,
    onBackClick: () -> Unit,
    onEditAvailabilityClick: () -> Unit,
    onLeft: () -> Unit,
    onLoginRequired: (resume: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.hasLeft) {
        if (state.hasLeft) onLeft()
    }

    LaunchedEffect(notice) {
        val current = notice ?: return@LaunchedEffect
        if (current == ParticipantManagementNotice.LOGIN_REQUIRED) {
            viewModel.clearNotice()
            onLoginRequired(viewModel::resumeAfterLogin)
            return@LaunchedEffect
        }
        val messageResource = when (current) {
            ParticipantManagementNotice.NOT_PARTICIPANT -> R.string.participant_management_not_participant
            ParticipantManagementNotice.HOST_MUST_DELEGATE -> R.string.participant_management_host_must_delegate
            ParticipantManagementNotice.LAST_MEMBER -> R.string.participant_management_last_member
            ParticipantManagementNotice.FAILED -> R.string.participant_management_failed
            ParticipantManagementNotice.FAILED_RETRY -> R.string.participant_management_failed_retry
            ParticipantManagementNotice.LOGIN_REQUIRED -> return@LaunchedEffect
        }
        snackbarHostState.showSnackbar(context.getString(messageResource))
        viewModel.clearNotice()
    }

    Box(modifier = modifier.fillMaxSize()) {
        ParticipantAppointmentManagementScreen(
            state = state.management,
            onBackClick = onBackClick,
            onEditAvailabilityClick = onEditAvailabilityClick,
            onNotificationsEnabledChange = viewModel::onNotificationsEnabledChange,
            onLeaveAppointmentClick = viewModel::requestLeave
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }

    if (state.isLeaveDialogVisible) {
        AlertDialog(
            onDismissRequest = viewModel::dismissLeave,
            title = { Text(stringResource(R.string.participant_management_leave_dialog_title)) },
            text = { Text(stringResource(R.string.participant_management_leave_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmLeave,
                    modifier = Modifier.testTag("participant-management-leave-confirm")
                ) {
                    Text(
                        stringResource(R.string.participant_management_leave_confirm),
                        color = AppColors.Danger.Red600
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissLeave) {
                    Text(stringResource(R.string.appointment_manage_dialog_cancel))
                }
            }
        )
    }
}
