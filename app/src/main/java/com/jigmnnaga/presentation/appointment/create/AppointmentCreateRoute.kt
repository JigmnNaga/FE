package com.jigmnnaga.presentation.appointment.create

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jigmnnaga.R

/**
 * Connects the UI-only APT-001 screen to its ViewModel and Android's clipboard/share sheet.
 * The parent still owns app-level navigation and supplies the exit action.
 */
@Composable
fun AppointmentCreateRoute(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppointmentCreateViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notice) {
        val messageResource = when (notice) {
            AppointmentCreateNotice.CREATE_API_NOT_CONNECTED -> R.string.appointment_create_api_unavailable
            AppointmentCreateNotice.INVITE_LINK_UNAVAILABLE -> R.string.appointment_invite_link_pending
            AppointmentCreateNotice.SHARE_UNAVAILABLE -> R.string.appointment_share_unavailable
            null -> null
        }
        if (messageResource != null) {
            snackbarHostState.showSnackbar(context.getString(messageResource))
            viewModel.clearNotice()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AppointmentCreateScreen(
            state = state,
            onStateChange = viewModel::updateUiState,
            onBack = onExit,
            onPrevious = { viewModel.movePrevious() },
            onNext = { viewModel.moveNext() },
            onCreate = viewModel::requestCreate,
            onShare = { action, link -> context.handleInviteAction(action, link, viewModel) },
            onDismissCopyConfirmation = viewModel::dismissCopyConfirmation
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

internal fun Context.handleInviteAction(
    action: AppointmentShareAction,
    link: String,
    viewModel: AppointmentCreateViewModel,
    hasShareTarget: ((Intent) -> Boolean)? = null
) {
    if (link.isBlank()) {
        viewModel.notifyShareUnavailable()
        return
    }

    when (action) {
        AppointmentShareAction.COPY_LINK -> runCatching {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.appointment_invite_link_label), link))
        }.onSuccess {
            viewModel.onInviteCopied()
        }.onFailure {
            viewModel.notifyShareUnavailable()
        }

        AppointmentShareAction.KAKAO_TALK -> {
            val shareIntent = buildInviteShareIntent(link)
            val canShare = hasShareTarget?.invoke(shareIntent) ?: hasInviteShareTarget(shareIntent)
            if (!canShare) {
                viewModel.notifyShareUnavailable()
                return
            }
            runCatching {
                startActivity(
                    Intent.createChooser(
                        shareIntent,
                        getString(R.string.appointment_share_chooser_title)
                    )
                )
            }.onFailure {
                viewModel.notifyShareUnavailable()
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun Context.hasInviteShareTarget(intent: Intent): Boolean =
    packageManager.queryIntentActivities(
        intent,
        android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
    ).isNotEmpty()

internal fun buildInviteShareIntent(link: String): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, link)
    }
