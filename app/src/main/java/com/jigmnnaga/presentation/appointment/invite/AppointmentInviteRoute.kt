package com.jigmnnaga.presentation.appointment.invite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.component.AppButton
import com.jigmnnaga.core.designsystem.component.BackHeader
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.core.designsystem.theme.AppDimens
import com.jigmnnaga.domain.model.InviteUnavailableReason

/**
 * Feature entry point. The host supplies invitation data/link and owns login and navigation.
 * Call [AppointmentInviteViewModel.retryCurrentInvite] after login returns to resume this invite.
 */
@Composable
fun AppointmentInviteRoute(
    viewModel: AppointmentInviteViewModel,
    onBack: () -> Unit,
    onLoginRequired: (resumeInviteAfterLogin: () -> Unit) -> Unit,
    onOpenChat: (appointmentId: Long) -> Unit,
    onReturnHome: () -> Unit,
    inviteCode: String? = null,
    inviteUrl: String? = null
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(inviteCode, inviteUrl) {
        val resolvedCode = inviteCode?.takeIf(String::isNotBlank)
            ?: inviteUrl?.let(AppointmentInviteLinkParser::extractInviteCode)
        viewModel.loadInvite(resolvedCode.orEmpty())
    }

    when (val current = state) {
        AppointmentInviteFlowState.Idle,
        AppointmentInviteFlowState.Loading -> InviteStatusScreen(
            title = stringResource(R.string.invite_loading_title),
            message = stringResource(R.string.invite_loading_message),
            onBack = onBack
        )
        AppointmentInviteFlowState.LoginRequired -> InviteStatusScreen(
            title = stringResource(R.string.invite_login_required_title),
            message = stringResource(R.string.invite_login_required_message),
            actionLabel = stringResource(R.string.invite_login_action),
            onBack = onBack,
            onAction = { onLoginRequired(viewModel::retryCurrentInvite) }
        )
        AppointmentInviteFlowState.Expired -> InviteStatusScreen(
            title = stringResource(R.string.invite_unavailable_title),
            message = stringResource(R.string.invite_expired_message),
            onBack = onBack
        )
        AppointmentInviteFlowState.InvalidLink -> InviteStatusScreen(
            title = stringResource(R.string.invite_unavailable_title),
            message = stringResource(R.string.invite_invalid_link_message),
            onBack = onBack
        )
        is AppointmentInviteFlowState.Unavailable -> InviteStatusScreen(
            title = stringResource(R.string.invite_unavailable_title),
            message = stringResource(current.reason.messageResource()),
            onBack = onBack
        )
        is AppointmentInviteFlowState.LoadFailed -> InviteStatusScreen(
            title = stringResource(R.string.invite_load_failed_title),
            message = stringResource(R.string.invite_load_failed_message),
            actionLabel = if (current.retryable) stringResource(R.string.invite_retry) else null,
            onBack = onBack,
            onAction = if (current.retryable) viewModel::retryCurrentInvite else null
        )
        is AppointmentInviteFlowState.Invitation -> AppointmentInviteScreen(
            state = current.uiState,
            onBack = onBack,
            onAccept = viewModel::acceptInvite,
            onDecline = viewModel::declineInvite,
            onOpenChat = { current.uiState.appointmentId?.let(onOpenChat) },
            onReturnHome = onReturnHome
        )
    }
}

@Composable
private fun InviteStatusScreen(
    title: String,
    message: String,
    onBack: () -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        BackHeader(title = stringResource(R.string.invite_flow_title), onBackClick = onBack)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = AppDimens.Spacing20),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.Monotone.SoftBlack,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.Monotone.Gray700,
                    textAlign = TextAlign.Center
                )
                if (actionLabel != null && onAction != null) {
                    AppButton(
                        text = actionLabel,
                        onClick = onAction,
                        modifier = Modifier.fillMaxWidth().padding(top = AppDimens.Spacing8)
                    )
                }
            }
        }
    }
}

private fun InviteUnavailableReason.messageResource(): Int = when (this) {
    InviteUnavailableReason.NOT_FOUND -> R.string.invite_not_found_message
    InviteUnavailableReason.CANCELED -> R.string.invite_canceled_message
    InviteUnavailableReason.KICKED -> R.string.invite_kicked_message
    InviteUnavailableReason.HOST_CANNOT_RESPOND -> R.string.invite_host_cannot_respond_message
    InviteUnavailableReason.OTHER -> R.string.invite_unavailable_message
}
