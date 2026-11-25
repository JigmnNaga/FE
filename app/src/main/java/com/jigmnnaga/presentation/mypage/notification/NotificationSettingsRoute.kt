package com.jigmnnaga.presentation.mypage.notification

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.jigmnnaga.R
import kotlinx.serialization.Serializable

/** 알림 설정 화면 네비게이션 목적지 타입. */
@Serializable
data object NotificationSettingsRoute

/**
 * 알림 설정 화면과 [NotificationSettingsViewModel]을 연결한다.
 * 저장에 실패하면 안내 문구를 한 번 띄운다.
 */
@Composable
fun NotificationSettingsRoute(
    viewModel: NotificationSettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saveFailed) {
        if (state.saveFailed) {
            viewModel.consumeSaveFailed()
            Toast.makeText(context, R.string.notification_settings_save_failed, Toast.LENGTH_SHORT).show()
        }
    }

    NotificationSettingsScreen(
        state = state,
        onBackClick = onBackClick,
        onAllNotificationsChange = viewModel::onAllNotificationsChange,
        onAppointmentReminderChange = viewModel::onAppointmentReminderChange,
        onDepartureTimeChange = viewModel::onDepartureTimeChange,
        onFriendArrivalChange = viewModel::onFriendArrivalChange,
        onMarketingChange = viewModel::onMarketingChange,
        modifier = modifier
    )
}
