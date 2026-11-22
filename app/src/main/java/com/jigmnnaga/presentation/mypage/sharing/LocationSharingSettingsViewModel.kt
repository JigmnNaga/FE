package com.jigmnnaga.presentation.mypage.sharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.LocationSharingScope
import com.jigmnnaga.domain.model.LocationSharingSettings
import com.jigmnnaga.domain.model.LocationSharingStartTime
import com.jigmnnaga.domain.model.LocationSharingSyncResult
import com.jigmnnaga.domain.repository.LocationSharingSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 위치 공유 설정 화면 상태를 만든다. 설정 값은 [repository]의 값이고, 화면을 열 때 서버 값으로 갱신한다.
 * 바꾼 값은 바로 보여 주고 서버에 저장하며, 저장에 실패하면 이전 값으로 돌아간 뒤 [LocationSharingSettingsUiState.saveFailed]로 알린다.
 * 위치 권한은 화면이 앞에 올 때마다 [onPermissionChecked]로 알려 준다.
 */
class LocationSharingSettingsViewModel(
    private val repository: LocationSharingSettingsRepository,
    initialPermissionGranted: Boolean = true
) : ViewModel() {

    private data class Signals(val saveFailed: Boolean = false, val loginRequired: Boolean = false)

    private val permissionGranted = MutableStateFlow(initialPermissionGranted)
    private val signals = MutableStateFlow(Signals())

    val uiState: StateFlow<LocationSharingSettingsUiState> =
        combine(repository.settings, permissionGranted, signals) { settings, granted, flags ->
            LocationSharingSettingsUiState(
                sharingEnabled = settings.sharingEnabled,
                scope = settings.scope,
                startTime = settings.startTime,
                locationPermissionGranted = granted,
                saveFailed = flags.saveFailed,
                loginRequired = flags.loginRequired
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LocationSharingSettingsUiState(locationPermissionGranted = initialPermissionGranted)
        )

    init {
        // 서버에 연결하지 못해도 기기에 저장된 값을 그대로 보여 주므로, 받기 실패는 따로 알리지 않는다.
        viewModelScope.launch { handle(repository.refresh(), reportFailure = false) }
    }

    fun onPermissionChecked(granted: Boolean) {
        permissionGranted.value = granted
    }

    fun onSharingEnabledChange(enabled: Boolean) = save { it.copy(sharingEnabled = enabled) }

    fun onScopeSelect(scope: LocationSharingScope) = save { it.copy(scope = scope) }

    fun onStartTimeSelect(startTime: LocationSharingStartTime) = save { it.copy(startTime = startTime) }

    fun consumeSaveFailed() = signals.update { it.copy(saveFailed = false) }

    fun consumeLoginRequired() = signals.update { it.copy(loginRequired = false) }

    private fun save(transform: (LocationSharingSettings) -> LocationSharingSettings) {
        viewModelScope.launch { handle(repository.update(transform), reportFailure = true) }
    }

    private fun handle(result: LocationSharingSyncResult, reportFailure: Boolean) {
        when (result) {
            LocationSharingSyncResult.Success -> Unit
            LocationSharingSyncResult.LoginRequired -> signals.update { it.copy(loginRequired = true) }
            is LocationSharingSyncResult.Failure -> if (reportFailure) signals.update { it.copy(saveFailed = true) }
        }
    }
}
