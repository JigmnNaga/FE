package com.jigmnnaga.presentation.mypage.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.NotificationSettingsSaveResult
import com.jigmnnaga.domain.repository.NotificationSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 알림 설정 화면 상태를 만든다. 설정 값은 [repository]의 DataStore 값이고,
 * 바꾼 값은 바로 보여 주고 기기에 저장하며, 저장에 실패하면 이전 값으로 돌아간 뒤
 * [NotificationSettingsUiState.saveFailed]로 알린다.
 *
 * BE 전역 알림 설정 API 미지원으로 현재는 로컬 DataStore에만 저장한다.
 * API 추가 시 [repository]에 서버 동기화 로직을 추가한다.
 */
class NotificationSettingsViewModel(
    private val repository: NotificationSettingsRepository
) : ViewModel() {

    private val saveFailed = MutableStateFlow(false)

    val uiState: StateFlow<NotificationSettingsUiState> =
        combine(repository.settings, saveFailed) { settings, failed ->
            NotificationSettingsUiState(
                allNotificationsEnabled = settings.allNotificationsEnabled,
                appointmentReminderEnabled = settings.appointmentReminderEnabled,
                departureTimeEnabled = settings.departureTimeEnabled,
                friendArrivalEnabled = settings.friendArrivalEnabled,
                marketingEnabled = settings.marketingEnabled,
                saveFailed = failed,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = NotificationSettingsUiState()
        )

    fun onAllNotificationsChange(enabled: Boolean) = save { it.copy(allNotificationsEnabled = enabled) }

    fun onAppointmentReminderChange(enabled: Boolean) = save { it.copy(appointmentReminderEnabled = enabled) }

    fun onDepartureTimeChange(enabled: Boolean) = save { it.copy(departureTimeEnabled = enabled) }

    fun onFriendArrivalChange(enabled: Boolean) = save { it.copy(friendArrivalEnabled = enabled) }

    fun onMarketingChange(enabled: Boolean) = save { it.copy(marketingEnabled = enabled) }

    fun consumeSaveFailed() = saveFailed.update { false }

    private fun save(transform: (com.jigmnnaga.domain.model.NotificationSettings) -> com.jigmnnaga.domain.model.NotificationSettings) {
        viewModelScope.launch {
            val result = repository.update(transform)
            if (result is NotificationSettingsSaveResult.Failure) {
                saveFailed.update { true }
            }
        }
    }
}
