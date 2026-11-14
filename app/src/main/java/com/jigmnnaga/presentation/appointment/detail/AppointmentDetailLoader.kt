package com.jigmnnaga.presentation.appointment.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.domain.model.AppointmentDetail

/**
 * 약속 상세를 불러오는 동안 로딩, 실패, 로그인 필요, 참여자 아님 화면을 보여 주고,
 * 불러오면 [content]에 약속 상세를 넘겨 실제 화면을 그린다.
 *
 * @param onLoginRequired 로그인 버튼을 눌렀을 때 호출한다. 로그인에 성공하면 실행할 일을 인자로 넘긴다.
 */
@Composable
fun AppointmentDetailLoader(
    viewModel: AppointmentDetailLoadViewModel,
    onBack: () -> Unit,
    onLoginRequired: (resumeAfterLogin: () -> Unit) -> Unit,
    content: @Composable (AppointmentDetail) -> Unit
) {
    val state by viewModel.state.collectAsState()

    when (val current = state) {
        AppointmentDetailLoadState.Loading -> LoadingScreen()
        is AppointmentDetailLoadState.Loaded -> content(current.detail)
        AppointmentDetailLoadState.LoginRequired -> MessageScreen(
            message = stringResource(R.string.appointment_detail_login_required),
            actionLabel = stringResource(R.string.appointment_detail_login_action),
            onAction = { onLoginRequired(viewModel::load) },
            onBack = onBack
        )
        AppointmentDetailLoadState.NotParticipant -> MessageScreen(
            message = stringResource(R.string.appointment_detail_not_participant),
            actionLabel = null,
            onAction = {},
            onBack = onBack
        )
        is AppointmentDetailLoadState.Failed -> MessageScreen(
            message = stringResource(R.string.appointment_detail_failed),
            actionLabel = if (current.retryable) stringResource(R.string.appointment_detail_retry) else null,
            onAction = viewModel::load,
            onBack = onBack
        )
    }
}

@Composable
private fun LoadingScreen() {
    val description = stringResource(R.string.appointment_detail_loading)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .safeDrawingPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = AppColors.MainGreen.Green700)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.MainGreen.Green900
        )
    }
}

@Composable
private fun MessageScreen(
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.MainGreen.Green900,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        if (actionLabel != null) {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.MainGreen.Green700)
            ) {
                Text(actionLabel)
            }
        }
        TextButton(onClick = onBack) {
            Text(
                text = stringResource(R.string.appointment_detail_back),
                color = AppColors.MainGreen.Green900
            )
        }
    }
}
