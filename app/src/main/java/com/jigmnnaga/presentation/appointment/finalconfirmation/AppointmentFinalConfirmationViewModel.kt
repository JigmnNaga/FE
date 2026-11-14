package com.jigmnnaga.presentation.appointment.finalconfirmation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.AttendanceAnswer
import com.jigmnnaga.domain.model.AttendanceRejectReason
import com.jigmnnaga.domain.model.AttendanceResult
import com.jigmnnaga.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 약속 확정 확인 화면에서 사용자에게 한 번 알려야 하는 결과. */
enum class AppointmentFinalConfirmationNotice {
    /** 참석 응답이 저장됨. 화면을 닫는다. */
    SUBMITTED,

    /** 로그인이 필요함. 로그인 화면으로 보낸다. */
    LOGIN_REQUIRED,
    NOT_PARTICIPANT,
    APPOINTMENT_CANCELED,
    NOT_CONFIRMED,
    TIME_PASSED,
    INVALID_ANSWER,
    FAILURE_RETRYABLE,
    FAILURE
}

/**
 * 확정된 약속을 확인하는 화면의 참석 여부 선택과 확인 버튼 활성 조건을 관리한다.
 * 확인을 누르면 선택한 참석 여부를 서버에 보내고 결과를 [notice]로 알린다.
 * 확정된 약속 정보는 호출하는 쪽이 [initialState]로 넘긴다.
 */
class AppointmentFinalConfirmationViewModel(
    private val appointmentId: Long,
    initialState: AppointmentFinalConfirmationUiState,
    private val attendanceRepository: AttendanceRepository
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

    /** 확인할 수 있을 때만 선택한 참석 여부를 서버로 보낸다. 실패해도 선택값은 유지한다. */
    fun onConfirm() {
        val current = _uiState.value
        if (!current.canConfirm) return
        val answer = when (current.attendanceChoice) {
            AppointmentAttendanceChoice.ATTENDING -> AttendanceAnswer.ATTENDING
            AppointmentAttendanceChoice.NOT_ATTENDING -> AttendanceAnswer.NOT_ATTENDING
            null -> return
        }
        _uiState.value = current.copy(isSubmitting = true).withConfirmEligibility()
        viewModelScope.launch {
            val result = attendanceRepository.respond(appointmentId, answer)
            _uiState.value = _uiState.value.copy(isSubmitting = false).withConfirmEligibility()
            _notice.value = result.toNotice()
        }
    }

    fun clearNotice() {
        _notice.value = null
    }

    private fun AttendanceResult<Unit>.toNotice(): AppointmentFinalConfirmationNotice = when (this) {
        is AttendanceResult.Success -> AppointmentFinalConfirmationNotice.SUBMITTED
        AttendanceResult.LoginRequired -> AppointmentFinalConfirmationNotice.LOGIN_REQUIRED
        AttendanceResult.NotParticipant -> AppointmentFinalConfirmationNotice.NOT_PARTICIPANT
        is AttendanceResult.Rejected -> when (reason) {
            AttendanceRejectReason.APPOINTMENT_CANCELED -> AppointmentFinalConfirmationNotice.APPOINTMENT_CANCELED
            AttendanceRejectReason.NOT_CONFIRMED -> AppointmentFinalConfirmationNotice.NOT_CONFIRMED
            AttendanceRejectReason.TIME_PASSED -> AppointmentFinalConfirmationNotice.TIME_PASSED
            AttendanceRejectReason.INVALID_ANSWER -> AppointmentFinalConfirmationNotice.INVALID_ANSWER
        }
        is AttendanceResult.Failure ->
            if (retryable) AppointmentFinalConfirmationNotice.FAILURE_RETRYABLE
            else AppointmentFinalConfirmationNotice.FAILURE
    }

    /** 참석 여부를 골랐고 처리 중이 아닐 때만 확인할 수 있다. */
    private fun AppointmentFinalConfirmationUiState.withConfirmEligibility():
        AppointmentFinalConfirmationUiState = copy(
        canConfirm = attendanceChoice != null && !isSubmitting
    )
}
