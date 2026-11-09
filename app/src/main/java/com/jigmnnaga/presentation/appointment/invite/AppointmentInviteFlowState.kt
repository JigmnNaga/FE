package com.jigmnnaga.presentation.appointment.invite

import com.jigmnnaga.domain.model.InviteUnavailableReason

sealed interface AppointmentInviteFlowState {
    data object Idle : AppointmentInviteFlowState
    data object Loading : AppointmentInviteFlowState
    data object LoginRequired : AppointmentInviteFlowState
    data object Expired : AppointmentInviteFlowState
    data object InvalidLink : AppointmentInviteFlowState
    data class Unavailable(val reason: InviteUnavailableReason) : AppointmentInviteFlowState
    data class LoadFailed(val retryable: Boolean) : AppointmentInviteFlowState
    data class Invitation(val uiState: AppointmentInviteUiState) : AppointmentInviteFlowState
}
