package com.jigmnnaga.presentation.mypage.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.NotificationSettings
import com.jigmnnaga.domain.model.NotificationSettingsSyncResult
import com.jigmnnaga.domain.repository.NotificationSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 알림 설정 화면 상태를 만든다. 설정 값은 [repository]의 DataStore 값이고, 화면을 열 때 서버 값으로 갱신한다.
 * 바꾼 값은 바로 보여 주고 서버에 저장하며, 저장에 실패하면 이전 값으로 돌아간 뒤
 * [NotificationSettingsUiState.saveFailed]로 알린다.
 * 연속 토글 변경은 Mutex로 직렬화하여 요청 순서와 무관하게 최신 값이 유지된다.
 */
class NotificationSettingsViewModel(
    private val repository: NotificationSettingsRepository,
) : ViewModel() {

    private data class Signals(val saveFailed: Boolean = false, val loginRequired: Boolean = false)

    private val signals = MutableStateFlow(Signals())

    val uiState: StateFlow<NotificationSettingsUiState> =
        combine(repository.settings, signals) { settings, flags ->
            NotificationSettingsUiState(
                allNotificationsEnabled = settings.allNotificationsEnabled,
                appointmentReminderEnabled = settings.appointmentReminderEnabled,
                departureTimeEnabled = settings.departureTimeEnabled,
                friendArrivalEnabled = settings.friendArrivalEnabled,
                marketingEnabled = settings.marketingEnabled,
                saveFailed = flags.saveFailed,
                loginRequired = flags.loginRequired,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = NotificationSettingsUiState(),
        )

    init {
        // 서버에 연결하지 못해도 기기에 저장된 값을 그대로 보여 주므로, 받기 실패는 따로 알리지 않는다.
        viewModelScope.launch { handle(repository.refresh(), reportFailure = false) }
    }

    fun onAllNotificationsChange(enabled: Boolean) = save { it.copy(allNotificationsEnabled = enabled) }

    fun onAppointmentReminderChange(enabled: Boolean) = save { it.copy(appointmentReminderEnabled = enabled) }

    fun onDepartureTimeChange(enabled: Boolean) = save { it.copy(departureTimeEnabled = enabled) }

    fun onFriendArrivalChange(enabled: Boolean) = save { it.copy(friendArrivalEnabled = enabled) }

    fun onMarketingChange(enabled: Boolean) = save { it.copy(marketingEnabled = enabled) }

    fun consumeSaveFailed() = signals.update { it.copy(saveFailed = false) }

    fun consumeLoginRequired() = signals.update { it.copy(loginRequired = false) }

    private fun save(transform: (NotificationSettings) -> NotificationSettings) {
        viewModelScope.launch { handle(repository.update(transform), reportFailure = true) }
    }

    private fun handle(result: NotificationSettingsSyncResult, reportFailure: Boolean) {
        when (result) {
            NotificationSettingsSyncResult.Success -> Unit
            NotificationSettingsSyncResult.LoginRequired -> signals.update { it.copy(loginRequired = true) }
            is NotificationSettingsSyncResult.Failure -> if (reportFailure) signals.update { it.copy(saveFailed = true) }
        }
    }
}
