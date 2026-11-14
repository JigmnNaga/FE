package com.jigmnnaga.presentation.appointment.host

/** A participant row supplied by the host-room feature or a future repository. */
data class HostRoomParticipantUiState(
    val id: String,
    val name: String,
    val statusLabel: String? = null,
    val isHost: Boolean = false
)

/** A chat row supplied by the host-room feature. No sample messages are created by the screen. */
data class HostRoomMessageUiState(
    val id: String,
    val senderName: String? = null,
    val body: String,
    val timeLabel: String? = null,
    val isMine: Boolean = false,
    val unreadCount: Int = 0,
    val isSystem: Boolean = false
)

enum class HostAppointmentRoomTab {
    CHAT,
    MAP_STATUS
}

/**
 * Render state for the host appointment room. Service lifecycle policy is deliberately
 * represented by display data so this UI does not invent server statuses or transitions.
 */
data class HostAppointmentRoomUiState(
    val title: String,
    val subtitle: String,
    val scheduleStatusLabel: String? = null,
    val dateSummary: String? = null,
    val placeSummary: String? = null,
    val responseCount: Int = 0,
    val participantCount: Int = 0,
    val participants: List<HostRoomParticipantUiState> = emptyList(),
    val messages: List<HostRoomMessageUiState> = emptyList(),
    val systemNotice: String? = null,
    val draftMessage: String = "",
    val selectedTab: HostAppointmentRoomTab = HostAppointmentRoomTab.CHAT,
    val isSummaryExpanded: Boolean = false
)
