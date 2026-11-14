package com.jigmnnaga.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AppointmentTitleUpdateRequestDto(val title: String)

@Serializable
data class CandidateDatesUpdateRequestDto(
    val dateSelectionType: String,
    val candidateDates: List<String>
)

@Serializable
data class CandidateLocationsUpdateRequestDto(
    val keepLocationIds: List<Long>,
    val newLocations: List<CreateCandidateLocationDto>
)

@Serializable
data class ResponseDeadlineUpdateRequestDto(val responseDeadline: String)

@Serializable
data class HostDelegateRequestDto(val memberId: Long)

/** 약속방 나가기 요청. 방장이 나갈 때만 [newHostMemberId]를 보낸다. */
@Serializable
data class AppointmentLeaveRequestDto(val newHostMemberId: Long? = null)

@Serializable
data class NotificationSettingRequestDto(val enabled: Boolean)

@Serializable
data class ReminderSendResponseDto(
    val sentCount: Int = 0,
    val reminders: List<ReminderResultDto> = emptyList()
)

@Serializable
data class ReminderResultDto(
    val type: String,
    val sentCount: Int = 0
)
