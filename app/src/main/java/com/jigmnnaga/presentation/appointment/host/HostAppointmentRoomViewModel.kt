package com.jigmnnaga.presentation.appointment.host

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 방장 약속방에서 사용자에게 한 번 알려야 하는 안내. */
enum class HostAppointmentRoomNotice {
    CHAT_API_NOT_CONNECTED
}

/**
 * 방장 약속방 화면의 탭, 요약 카드, 메시지 입력 상태를 관리한다.
 * 채팅 서버 연동 전이라 메시지는 전송하지 않고, 입력값을 유지한 채 안내만 보낸다.
 * 약속 정보와 참여자 목록은 호출하는 쪽에서 [initialState]로 넘긴다.
 */
class HostAppointmentRoomViewModel(
    initialState: HostAppointmentRoomUiState
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<HostAppointmentRoomUiState> = _uiState.asStateFlow()

    private val _notice = MutableStateFlow<HostAppointmentRoomNotice?>(null)
    val notice: StateFlow<HostAppointmentRoomNotice?> = _notice.asStateFlow()

    fun onTabSelected(tab: HostAppointmentRoomTab) {
        edit { copy(selectedTab = tab) }
    }

    fun onSummaryExpandedChange(expanded: Boolean) {
        edit { copy(isSummaryExpanded = expanded) }
    }

    fun onMessageChange(message: String) {
        edit { copy(draftMessage = message) }
    }

    /** 공백만 있는 입력은 무시한다. 서버 연동 전에는 입력값을 지우지 않고 안내만 띄운다. */
    fun onSendMessage() {
        if (_uiState.value.draftMessage.isBlank()) return
        _notice.value = HostAppointmentRoomNotice.CHAT_API_NOT_CONNECTED
    }

    fun clearNotice() {
        _notice.value = null
    }

    /** 서버나 저장소에서 받은 약속 정보를 반영할 때 쓴다. 탭·입력값 같은 화면 상태는 유지한다. */
    fun onRoomInfoChanged(newState: HostAppointmentRoomUiState) {
        edit {
            newState.copy(
                draftMessage = draftMessage,
                selectedTab = selectedTab,
                isSummaryExpanded = isSummaryExpanded
            )
        }
    }

    private fun edit(transform: HostAppointmentRoomUiState.() -> HostAppointmentRoomUiState) {
        _uiState.value = _uiState.value.transform()
    }
}
