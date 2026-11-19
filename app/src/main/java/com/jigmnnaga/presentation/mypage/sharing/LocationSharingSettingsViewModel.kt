package com.jigmnnaga.presentation.mypage.sharing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.LocationSharingScope
import com.jigmnnaga.domain.model.LocationSharingStartTime
import com.jigmnnaga.domain.repository.LocationSharingSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 위치 공유 설정 화면 상태를 만든다. 설정 값은 [repository]에 저장된 값이고,
 * 위치 권한은 화면이 앞에 올 때마다 [onPermissionChecked]로 알려 준다.
 */
class LocationSharingSettingsViewModel(
    private val repository: LocationSharingSettingsRepository,
    initialPermissionGranted: Boolean = true
) : ViewModel() {

    private val permissionGranted = MutableStateFlow(initialPermissionGranted)

    val uiState: StateFlow<LocationSharingSettingsUiState> =
        combine(repository.settings, permissionGranted) { settings, granted ->
            LocationSharingSettingsUiState(
                sharingEnabled = settings.sharingEnabled,
                scope = settings.scope,
                startTime = settings.startTime,
                locationPermissionGranted = granted
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LocationSharingSettingsUiState(locationPermissionGranted = initialPermissionGranted)
        )

    fun onPermissionChecked(granted: Boolean) {
        permissionGranted.value = granted
    }

    fun onSharingEnabledChange(enabled: Boolean) {
        viewModelScope.launch { repository.setSharingEnabled(enabled) }
    }

    fun onScopeSelect(scope: LocationSharingScope) {
        viewModelScope.launch { repository.setScope(scope) }
    }

    fun onStartTimeSelect(startTime: LocationSharingStartTime) {
        viewModelScope.launch { repository.setStartTime(startTime) }
    }
}
