package com.jigmnnaga.presentation.appointment.route

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 이동 경로 화면(ROU-001)의 단계 전환을 맡는다. 경로 조회·출발 기록·위치 공유 같은 서버 연동은
 * 후속 작업에서 붙이고, 지금은 화면 안의 선택과 펼침만 다룬다.
 */
class RouteViewModel(
    initialState: RouteUiState = RouteUiState()
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<RouteUiState> = _uiState.asStateFlow()

    /** 초기화면에서 내 카드를 눌렀을 때. */
    fun onMyCardClick() = _uiState.update { it.openRouteSelection() }

    fun onSelectTransport(transport: RouteTransport) = _uiState.update { it.selectTransport(transport) }

    fun onToggleOption(transport: RouteTransport) = _uiState.update { it.toggleOptionExpanded(transport) }

    fun onStartMoving() = _uiState.update { it.startMoving() }

    fun onToggleMyRoute() = _uiState.update { it.toggleMyRouteExpanded() }

    fun onArrive() = _uiState.update { it.arrive() }

    /**
     * 뒤로 가기. 경로 선택 단계에서는 초기화면으로 돌아가고 true를 돌려 준다.
     * 그 밖의 단계에서는 처리하지 않고 false를 돌려 주어 호출한 쪽이 화면을 닫게 한다.
     */
    fun onBack(): Boolean {
        val current = _uiState.value
        if (current.stage != RouteStage.SELECT) return false
        _uiState.value = current.closeRouteSelection()
        return true
    }
}
