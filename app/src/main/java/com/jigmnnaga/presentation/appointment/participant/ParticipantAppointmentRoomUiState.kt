package com.jigmnnaga.presentation.appointment.participant

/** Only the two room states present in the participant Figma frames are modeled here. */
enum class ParticipantAppointmentStatus {
    AWAITING_CONFIRMATION,
    CONFIRMED
}

enum class ParticipantAppointmentRoomTab {
    CHAT,
    MAP_STATUS
}

data class ParticipantRoomMemberUiState(
    val id: String,
    val name: String,
    val statusLabel: String? = null,
    val isCurrentUser: Boolean = false
)

data class ParticipantRoomMessageUiState(
    val id: String,
    val body: String,
    val senderName: String? = null,
    val timeLabel: String? = null,
    val isMine: Boolean = false
)

data class ParticipantAppointmentRoomUiState(
    val title: String,
    val subtitle: String,
    val appointmentStatus: ParticipantAppointmentStatus,
    val scheduleStatusLabel: String? = null,
    val dateSummary: String? = null,
    val placeSummary: String? = null,
    val members: List<ParticipantRoomMemberUiState> = emptyList(),
    val messages: List<ParticipantRoomMessageUiState> = emptyList(),
    val draftMessage: String = "",
    val selectedTab: ParticipantAppointmentRoomTab = ParticipantAppointmentRoomTab.CHAT,
    val isSummaryExpanded: Boolean = false,
    val canReportDeparture: Boolean = false
)

data class ParticipantAppointmentManagementUiState(
    val appointmentName: String,
    val scheduleStatusLabel: String,
    val dateSummary: String? = null,
    val placeSummary: String? = null,
    val members: List<ParticipantRoomMemberUiState> = emptyList(),
    val myAvailabilitySummary: String? = null,
    val isNotificationsEnabled: Boolean = true
)
