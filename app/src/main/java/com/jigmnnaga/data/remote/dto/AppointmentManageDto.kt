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
