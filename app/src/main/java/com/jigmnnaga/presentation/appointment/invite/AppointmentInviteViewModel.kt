package com.jigmnnaga.presentation.appointment.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.domain.model.AppointmentInvite
import com.jigmnnaga.domain.model.InviteOperationResult
import com.jigmnnaga.domain.model.InviteParticipant
import com.jigmnnaga.domain.model.InviteParticipantRole
import com.jigmnnaga.domain.model.InviteParticipationStatus
import com.jigmnnaga.domain.model.InviteUnavailableReason
import com.jigmnnaga.domain.repository.AppointmentInviteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppointmentInviteViewModel(
    private val repository: AppointmentInviteRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _state = MutableStateFlow<AppointmentInviteFlowState>(AppointmentInviteFlowState.Idle)
    val state: StateFlow<AppointmentInviteFlowState> = _state.asStateFlow()

    private var currentInviteCode: String? = null
    private var requestJob: Job? = null

    fun loadInvite(inviteCode: String) {
        if (inviteCode.isBlank()) {
            currentInviteCode = null
            requestJob?.cancel()
            _state.value = AppointmentInviteFlowState.InvalidLink
            return
        }
        currentInviteCode = inviteCode
        loadCurrentInvite()
    }

    /** Call after the host's login flow returns; the invite code remains in this ViewModel. */
    fun retryCurrentInvite() {
        if (currentInviteCode != null) loadCurrentInvite()
    }

    fun acceptInvite() = submitDecision(accept = true)

    fun declineInvite() = submitDecision(accept = false)

    private fun loadCurrentInvite() {
        val inviteCode = currentInviteCode ?: return
        if (_state.value == AppointmentInviteFlowState.Loading && requestJob?.isActive == true) return

        requestJob?.cancel()
        _state.value = AppointmentInviteFlowState.Loading
        requestJob = viewModelScope.launch(dispatcher) {
            when (val result = runRepositoryCall { repository.getInvite(inviteCode) }) {
                is InviteOperationResult.Success -> _state.value = result.value.toFlowState()
                InviteOperationResult.LoginRequired -> _state.value = AppointmentInviteFlowState.LoginRequired
                InviteOperationResult.Expired -> _state.value = AppointmentInviteFlowState.Expired
                InviteOperationResult.AlreadyResponded -> reloadAfterAlreadyResponded(inviteCode)
                is InviteOperationResult.Unavailable -> _state.value =
                    AppointmentInviteFlowState.Unavailable(result.reason)
                is InviteOperationResult.Failure -> _state.value =
                    AppointmentInviteFlowState.LoadFailed(retryable = result.retryable)
            }
        }
    }

    private fun submitDecision(accept: Boolean) {
        val current = _state.value as? AppointmentInviteFlowState.Invitation ?: return
        val inviteCode = currentInviteCode ?: return
        if (current.uiState.isSubmitting || current.uiState.result != AppointmentInviteResult.PENDING) return

        _state.value = AppointmentInviteFlowState.Invitation(
            current.uiState.copy(isSubmitting = true, decisionError = null)
        )
        requestJob?.cancel()
        requestJob = viewModelScope.launch(dispatcher) {
            val result = runRepositoryCall {
                if (accept) {
                    repository.acceptInvite(inviteCode).mapSuccess { it }
                } else {
                    repository.declineInvite(inviteCode).mapSuccess { null }
                }
            }
            when (result) {
                is InviteOperationResult.Success -> {
                    val currentInvite = (_state.value as? AppointmentInviteFlowState.Invitation)?.uiState
                        ?: current.uiState
                    _state.value = AppointmentInviteFlowState.Invitation(
                        currentInvite.copy(
                            result = if (accept) AppointmentInviteResult.ACCEPTED else AppointmentInviteResult.DECLINED,
                            appointmentId = result.value ?: currentInvite.appointmentId,
                            isSubmitting = false,
                            decisionError = null
                        )
                    )
                }
                InviteOperationResult.LoginRequired -> _state.value = AppointmentInviteFlowState.LoginRequired
                InviteOperationResult.Expired -> _state.value = AppointmentInviteFlowState.Expired
                InviteOperationResult.AlreadyResponded -> reloadAfterAlreadyResponded(inviteCode)
                is InviteOperationResult.Unavailable -> _state.value =
                    AppointmentInviteFlowState.Unavailable(result.reason)
                is InviteOperationResult.Failure -> {
                    if (result.retryable) {
                        val currentInvite = (_state.value as? AppointmentInviteFlowState.Invitation)?.uiState
                            ?: current.uiState
                        _state.value = AppointmentInviteFlowState.Invitation(
                            currentInvite.copy(
                                isSubmitting = false,
                                decisionError = AppointmentInviteDecisionError.REQUEST_FAILED
                            )
                        )
                    } else {
                        _state.value = AppointmentInviteFlowState.Unavailable(InviteUnavailableReason.OTHER)
                    }
                }
            }
        }
    }

    private suspend fun reloadAfterAlreadyResponded(inviteCode: String) {
        _state.value = AppointmentInviteFlowState.Loading
        when (val result = runRepositoryCall { repository.getInvite(inviteCode) }) {
            is InviteOperationResult.Success -> _state.value = result.value.toFlowState()
            InviteOperationResult.LoginRequired -> _state.value = AppointmentInviteFlowState.LoginRequired
            InviteOperationResult.Expired -> _state.value = AppointmentInviteFlowState.Expired
            InviteOperationResult.AlreadyResponded -> _state.value =
                AppointmentInviteFlowState.Unavailable(InviteUnavailableReason.OTHER)
            is InviteOperationResult.Unavailable -> _state.value =
                AppointmentInviteFlowState.Unavailable(result.reason)
            is InviteOperationResult.Failure -> _state.value =
                AppointmentInviteFlowState.LoadFailed(retryable = result.retryable)
        }
    }

    private suspend fun <T> runRepositoryCall(block: suspend () -> InviteOperationResult<T>): InviteOperationResult<T> =
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            InviteOperationResult.Failure(retryable = true)
        }

    private fun AppointmentInvite.toFlowState(): AppointmentInviteFlowState {
        if (expired) return AppointmentInviteFlowState.Expired
        if (canceled) return AppointmentInviteFlowState.Unavailable(InviteUnavailableReason.CANCELED)
        if (myRole == InviteParticipantRole.HOST) {
            return AppointmentInviteFlowState.Unavailable(InviteUnavailableReason.HOST_CANNOT_RESPOND)
        }
        if (myStatus == InviteParticipationStatus.KICKED) {
            return AppointmentInviteFlowState.Unavailable(InviteUnavailableReason.KICKED)
        }
        if (myStatus == InviteParticipationStatus.OTHER) {
            return AppointmentInviteFlowState.Unavailable(InviteUnavailableReason.OTHER)
        }

        val result = when (myStatus) {
            InviteParticipationStatus.ACCEPTED -> AppointmentInviteResult.ACCEPTED
            InviteParticipationStatus.DECLINED -> AppointmentInviteResult.DECLINED
            InviteParticipationStatus.PENDING -> AppointmentInviteResult.PENDING
            InviteParticipationStatus.KICKED,
            InviteParticipationStatus.OTHER -> return AppointmentInviteFlowState.Unavailable(
                InviteUnavailableReason.OTHER
            )
        }

        return AppointmentInviteFlowState.Invitation(
            AppointmentInviteUiState(
                appointmentName = title,
                inviterName = hostNickname.orEmpty(),
                members = participants.map { it.toUiMember() },
                result = result,
                appointmentId = appointmentId
            )
        )
    }

    private fun InviteParticipant.toUiMember() = AppointmentInviteMember(
        name = nickname,
        status = when {
            role == InviteParticipantRole.HOST -> AppointmentInviteMemberStatus.HOST
            else -> when (status) {
                InviteParticipationStatus.ACCEPTED -> AppointmentInviteMemberStatus.ACCEPTED
                InviteParticipationStatus.DECLINED -> AppointmentInviteMemberStatus.DECLINED
                InviteParticipationStatus.PENDING -> AppointmentInviteMemberStatus.WAITING
                InviteParticipationStatus.KICKED,
                InviteParticipationStatus.OTHER -> AppointmentInviteMemberStatus.UNKNOWN
            }
        }
    )
}

private fun <T> InviteOperationResult<T>.mapSuccess(
    transform: (T) -> Long?
): InviteOperationResult<Long?> = when (this) {
    is InviteOperationResult.Success -> InviteOperationResult.Success(transform(value))
    InviteOperationResult.LoginRequired -> InviteOperationResult.LoginRequired
    InviteOperationResult.Expired -> InviteOperationResult.Expired
    InviteOperationResult.AlreadyResponded -> InviteOperationResult.AlreadyResponded
    is InviteOperationResult.Unavailable -> this
    is InviteOperationResult.Failure -> this
}
