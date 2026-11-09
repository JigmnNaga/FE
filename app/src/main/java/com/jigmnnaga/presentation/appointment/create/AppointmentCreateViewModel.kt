package com.jigmnnaga.presentation.appointment.create

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** User-visible one-shot feedback emitted by the local appointment flow. */
enum class AppointmentCreateNotice {
    CREATE_API_NOT_CONNECTED,
    INVITE_LINK_UNAVAILABLE,
    SHARE_UNAVAILABLE
}

/**
 * Owns the APT-001 form state and step transitions. The create endpoint and its contract are not
 * available yet, so this ViewModel only prepares a validated local draft and never fabricates a
 * successful appointment or invitation link.
 */
class AppointmentCreateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppointmentCreateUiState())
    val uiState: StateFlow<AppointmentCreateUiState> = _uiState.asStateFlow()

    private val _pendingDraft = MutableStateFlow<AppointmentCreateDraft?>(null)
    val pendingDraft: StateFlow<AppointmentCreateDraft?> = _pendingDraft.asStateFlow()

    private val _notice = MutableStateFlow<AppointmentCreateNotice?>(null)
    val notice: StateFlow<AppointmentCreateNotice?> = _notice.asStateFlow()

    /** Receives immutable form updates from the existing UI composable. */
    fun updateUiState(state: AppointmentCreateUiState) {
        val current = _uiState.value
        val normalized = state.copy(
            selectedLocations = state.selectedLocations.distinct().take(MAX_SELECTED_LOCATIONS),
            selectedDates = state.selectedDates.distinct().sorted()
        )
        val formChanged = current.appointmentName != normalized.appointmentName ||
            current.scheduleType != normalized.scheduleType ||
            current.selectedStartDate != normalized.selectedStartDate ||
            current.selectedEndDate != normalized.selectedEndDate ||
            current.selectedDates != normalized.selectedDates ||
            current.selectedLocations != normalized.selectedLocations ||
            current.deadlineChoice != normalized.deadlineChoice ||
            current.customDeadline != normalized.customDeadline

        _uiState.value = normalized.copy(
            submissionStatus = if (formChanged) AppointmentCreateSubmissionStatus.READY
            else current.submissionStatus
        )
        if (formChanged) {
            _pendingDraft.value = null
            _notice.value = null
        }
    }

    /** Advances only when the current step is valid; all entered values remain in UiState. */
    fun moveNext(): Boolean {
        val state = _uiState.value
        if (!state.isCurrentStepValid()) return false
        val nextStep = when (state.step) {
            AppointmentCreateStep.APPOINTMENT_NAME -> AppointmentCreateStep.SCHEDULE
            AppointmentCreateStep.SCHEDULE -> AppointmentCreateStep.LOCATIONS
            AppointmentCreateStep.LOCATIONS -> AppointmentCreateStep.RESPONSE_DEADLINE
            AppointmentCreateStep.RESPONSE_DEADLINE,
            AppointmentCreateStep.INVITATION -> return false
        }
        _uiState.value = state.copy(step = nextStep)
        return true
    }

    /** Returns to the previous input step without clearing any input. */
    fun movePrevious(): Boolean {
        val state = _uiState.value
        val previousStep = when (state.step) {
            AppointmentCreateStep.APPOINTMENT_NAME -> return false
            AppointmentCreateStep.SCHEDULE -> AppointmentCreateStep.APPOINTMENT_NAME
            AppointmentCreateStep.LOCATIONS -> AppointmentCreateStep.SCHEDULE
            AppointmentCreateStep.RESPONSE_DEADLINE -> AppointmentCreateStep.LOCATIONS
            AppointmentCreateStep.INVITATION -> AppointmentCreateStep.RESPONSE_DEADLINE
        }
        _uiState.value = state.copy(step = previousStep)
        return true
    }

    /** Builds the payload shape for a future API integration without assigning an ID or URL. */
    fun buildDraft(): AppointmentCreateDraft? = _uiState.value.toDraftOrNull()

    /** Stores the local draft and explicitly reports that no server submission took place. */
    fun requestCreate(draftFromUi: AppointmentCreateDraft) {
        val current = _uiState.value
        if (current.submissionStatus != AppointmentCreateSubmissionStatus.READY || current.isCreating) return
        val validatedDraft = current.toDraftOrNull()
        val normalizedUiDraft = draftFromUi.copy(
            name = draftFromUi.name.trim(),
            specificDates = draftFromUi.specificDates.distinct().sorted(),
            locations = draftFromUi.locations.distinct().take(MAX_SELECTED_LOCATIONS)
        )
        if (validatedDraft == null || validatedDraft != normalizedUiDraft) return
        _pendingDraft.value = validatedDraft
        // No API call is made here, so this is a terminal unavailable state rather than a spinner.
        _uiState.value = current.copy(
            isCreating = false,
            submissionStatus = AppointmentCreateSubmissionStatus.API_UNAVAILABLE
        )
        _notice.value = AppointmentCreateNotice.CREATE_API_NOT_CONNECTED
    }

    /** Integration point for a real successful backend response. Blank links never show success. */
    fun onInviteLinkIssued(inviteLink: String) {
        if (inviteLink.isBlank()) {
            _notice.value = AppointmentCreateNotice.INVITE_LINK_UNAVAILABLE
            return
        }
        _uiState.value = _uiState.value.copy(
            step = AppointmentCreateStep.INVITATION,
            isCreating = false,
            submissionStatus = AppointmentCreateSubmissionStatus.SUCCEEDED,
            inviteLink = inviteLink,
            copyConfirmationVisible = false
        )
        _pendingDraft.value = null
    }

    fun onInviteCopied() {
        if (_uiState.value.inviteLink.isNullOrBlank()) {
            _notice.value = AppointmentCreateNotice.INVITE_LINK_UNAVAILABLE
            return
        }
        _uiState.value = _uiState.value.copy(copyConfirmationVisible = true)
    }

    fun dismissCopyConfirmation() {
        _uiState.value = _uiState.value.copy(copyConfirmationVisible = false)
    }

    fun notifyShareUnavailable() {
        _notice.value = AppointmentCreateNotice.SHARE_UNAVAILABLE
    }

    fun clearNotice() {
        _notice.value = null
    }
}
