package com.jigmnnaga.presentation.mypage.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable

/** 알림 설정 화면 네비게이션 목적지 타입. */
@Serializable
data object NotificationSettingsRoute

/**
 * 알림 설정 화면 Route Composable.
 *
 * ViewModel 연동 전까지는 로컬 임시 상태로 토글 UI를 확인한다.
 * API 연동 시 NotificationSettingsViewModel을 생성해 실제 데이터를 공급하면 된다.
 */
@Composable
fun NotificationSettingsRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var state by remember { mutableStateOf(NotificationSettingsUiState()) }

    NotificationSettingsScreen(
        state = state,
        onBackClick = onBackClick,
        onAllNotificationsChange = { state = state.copy(allNotificationsEnabled = it) },
        onAppointmentReminderChange = { state = state.copy(appointmentReminderEnabled = it) },
        onDepartureTimeChange = { state = state.copy(departureTimeEnabled = it) },
        onFriendArrivalChange = { state = state.copy(friendArrivalEnabled = it) },
        onMarketingChange = { state = state.copy(marketingEnabled = it) },
        modifier = modifier
    )
}
