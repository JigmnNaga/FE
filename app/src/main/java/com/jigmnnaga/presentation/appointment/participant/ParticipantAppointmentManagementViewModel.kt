package com.jigmnnaga.presentation.appointment.participant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.AppointmentManageRejectReason
import com.jigmnnaga.domain.model.AppointmentManageResult
import com.jigmnnaga.domain.repository.AppointmentManageRepository
import com.jigmnnaga.presentation.appointment.AppointmentRefreshSignal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 참여자 약속방 관리 화면이 한 번 보여 주는 안내. */
enum class ParticipantManagementNotice {
    /** 로그인이 필요하다. 화면이 로그인으로 보낸 뒤 [ParticipantAppointmentManagementViewModel.resumeAfterLogin]을 부른다. */
    LOGIN_REQUIRED,
    NOT_PARTICIPANT,

    /** 방장은 다음 방장을 정해야 나갈 수 있다. */
    HOST_MUST_DELEGATE,

    /** 혼자 남은 약속방은 나갈 수 없다. */
    LAST_MEMBER,
    FAILED,
    FAILED_RETRY
}

/** [management]는 기존 관리 화면이 그리는 값이고, 나머지는 서버 요청 진행 상태다. */
data class ParticipantManagementState(
    val management: ParticipantAppointmentManagementUiState,
    val isLeaveDialogVisible: Boolean = false,
    val isWorking: Boolean = false,
    val hasLeft: Boolean = false
)

/**
 * 참여자가 약속방 알림을 켜고 끄거나 약속방을 나가는 화면의 상태를 관리한다.
 * 알림 설정은 먼저 화면에 반영하고, 서버가 받아 주지 않으면 이전 값으로 되돌린다.
 * 나가기는 확인 팝업에서 확인한 뒤에만 서버에 보내고, 성공하면 [ParticipantManagementState.hasLeft]로 알린다.
 *
 * @param onChanged 나가기가 성공했을 때 부른다. 약속 상세를 보여 주는 다른 화면이 다시 불러오는 데 쓴다.
 */
class ParticipantAppointmentManagementViewModel(
    private val appointmentId: Long,
    initialState: ParticipantAppointmentManagementUiState,
    private val repository: AppointmentManageRepository,
    private val onChanged: () -> Unit = AppointmentRefreshSignal::notifyChanged
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParticipantManagementState(management = initialState))
    val uiState: StateFlow<ParticipantManagementState> = _uiState.asStateFlow()

    private val _notice = MutableStateFlow<ParticipantManagementNotice?>(null)
    val notice: StateFlow<ParticipantManagementNotice?> = _notice.asStateFlow()

    private var loginResume: (() -> Unit)? = null

    fun onNotificationsEnabledChange(enabled: Boolean) {
        val state = _uiState.value
        val previous = state.management.isNotificationsEnabled
        if (state.isWorking || state.hasLeft || previous == enabled) return
        _uiState.value = state.copy(management = state.management.copy(isNotificationsEnabled = enabled))
        launchCall(
            retry = { onNotificationsEnabledChange(enabled) },
            onFailure = { edit { copy(management = management.copy(isNotificationsEnabled = previous)) } },
            call = { repository.updateNotificationSetting(appointmentId, enabled) }
        ) { }
    }

    fun requestLeave() {
        val state = _uiState.value
        if (state.isWorking || state.hasLeft) return
        _uiState.value = state.copy(isLeaveDialogVisible = true)
    }

    fun dismissLeave() {
        edit { copy(isLeaveDialogVisible = false) }
    }

    /** 나가기 확인 팝업에서 나가기를 눌렀을 때. */
    fun confirmLeave() {
        val state = _uiState.value
        if (!state.isLeaveDialogVisible || state.isWorking || state.hasLeft) return
        _uiState.value = state.copy(isLeaveDialogVisible = false)
        launchCall(
            retry = { requestLeave(); confirmLeave() },
            call = { repository.leave(appointmentId) }
        ) {
            onChanged()
            edit { copy(hasLeft = true) }
        }
    }

    /** 로그인에 성공한 뒤 하려던 일을 이어서 한다. */
    fun resumeAfterLogin() {
        val resume = loginResume
        loginResume = null
        resume?.invoke()
    }

    fun clearNotice() {
        _notice.value = null
    }

    private fun <T> launchCall(
        retry: () -> Unit,
        onFailure: () -> Unit = {},
        call: suspend () -> AppointmentManageResult<T>,
        onSuccess: (T) -> Unit
    ) {
        edit { copy(isWorking = true) }
        viewModelScope.launch {
            val result = call()
            edit { copy(isWorking = false) }
            if (result is AppointmentManageResult.Success) {
                onSuccess(result.value)
                return@launch
            }
            onFailure()
            _notice.value = when (result) {
                AppointmentManageResult.LoginRequired -> {
                    loginResume = retry
                    ParticipantManagementNotice.LOGIN_REQUIRED
                }
                AppointmentManageResult.NotHost -> ParticipantManagementNotice.NOT_PARTICIPANT
                is AppointmentManageResult.Rejected -> when (result.reason) {
                    AppointmentManageRejectReason.NEW_HOST_REQUIRED -> ParticipantManagementNotice.HOST_MUST_DELEGATE
                    AppointmentManageRejectReason.LAST_MEMBER_CANNOT_LEAVE -> ParticipantManagementNotice.LAST_MEMBER
                    else -> ParticipantManagementNotice.FAILED
                }
                is AppointmentManageResult.Failure ->
                    if (result.retryable) ParticipantManagementNotice.FAILED_RETRY else ParticipantManagementNotice.FAILED
                is AppointmentManageResult.Success -> return@launch
            }
        }
    }

    private fun edit(transform: ParticipantManagementState.() -> ParticipantManagementState) {
        _uiState.value = _uiState.value.transform()
    }
}
