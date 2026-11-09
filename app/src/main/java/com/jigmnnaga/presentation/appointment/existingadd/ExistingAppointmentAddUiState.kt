package com.jigmnnaga.presentation.appointment.existingadd

/**
 * UI-only states for the APT-006 screens. The host owns transitions and business rules.
 */
enum class ExistingAppointmentAddPage {
    RESCHEDULE,
    REQUEST_PARTICIPANTS
}

enum class ExistingAppointmentRoomTab {
    CHAT,
    MAP_AND_STATUS
}

data class ExistingAppointmentMemberUiState(
    val id: String,
    val name: String,
    val statusLabel: String,
    val detailLabel: String? = null,
    val trailingLabel: String? = null,
    val isSelectable: Boolean = true
)

data class ExistingAppointmentAddUiState(
    val page: ExistingAppointmentAddPage,
    val appointmentTitle: String,
    val memberSummary: String,
    val scheduledTime: String,
    val scheduledPlace: String,
    val members: List<ExistingAppointmentMemberUiState>,
    val selectedMemberIds: Set<String> = emptySet(),
    val selectedRoomTab: ExistingAppointmentRoomTab = ExistingAppointmentRoomTab.MAP_AND_STATUS,
    /** Optional caller-provided live status. Omit it when live updates are unavailable. */
    val liveStatusLabel: String? = null,
    /** The caller decides when its validation rules allow the request action. */
    val canRequest: Boolean = false
)
