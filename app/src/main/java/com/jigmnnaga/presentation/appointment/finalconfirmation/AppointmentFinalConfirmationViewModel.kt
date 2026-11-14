package com.jigmnnaga.presentation.appointment.finalconfirmation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 약속 확정 확인 화면에서 사용자에게 한 번 알려야 하는 안내. */
enum class AppointmentFinalConfirmationNotice {
    CONFIRM_API_NOT_CONNECTED
}

/**
 * 확정된 약속을 확인하는 화면의 참석 여부 선택과 확인 버튼 활성 조건을 관리한다.
 * 확정 확인 서버 연동 전이라 확인을 눌러도 서버로 보내지 않고, 선택값을 유지한 채 안내만 보낸다.
 * 확정된 약속 정보는 호출하는 쪽이 [initialState]로 넘긴다.
 */
class AppointmentFinalConfirmationViewModel(
    initialState: AppointmentFinalConfirmationUiState
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState.withConfirmEligibility())
    val uiState: StateFlow<AppointmentFinalConfirmationUiState> = _uiState.asStateFlow()

    private val _notice = MutableStateFlow<AppointmentFinalConfirmationNotice?>(null)
    val notice: StateFlow<AppointmentFinalConfirmationNotice?> = _notice.asStateFlow()

    /** 참석 여부를 고른다. 처리 중에는 바꾸지 않는다. */
    fun onAttendanceChoiceChange(choice: AppointmentAttendanceChoice) {
        if (_uiState.value.isSubmitting) return
        _uiState.value = _uiState.value.copy(attendanceChoice = choice).withConfirmEligibility()
    }

    /** 확인할 수 있을 때만 서버로 보내지 않고 안내만 보낸다. 선택값은 유지한다. */
    fun onConfirm() {
        if (!_uiState.value.canConfirm) return
        _notice.value = AppointmentFinalConfirmationNotice.CONFIRM_API_NOT_CONNECTED
    }

    fun clearNotice() {
        _notice.value = null
    }

    /** 참석 여부를 골랐고 처리 중이 아닐 때만 확인할 수 있다. */
    private fun AppointmentFinalConfirmationUiState.withConfirmEligibility():
        AppointmentFinalConfirmationUiState = copy(
        canConfirm = attendanceChoice != null && !isSubmitting
    )
}
