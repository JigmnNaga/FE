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
    /** 오른쪽에 크게 보이는 값. 예: "2분" */
    val trailingValue: String? = null,
    /** 큰 값 아래의 작은 설명. 예: "지각" */
    val trailingCaption: String? = null,
    /** 본인이면 아바타를 진한 초록색으로 표시한다. */
    val isMe: Boolean = false,
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