package com.jigmnnaga.data.remote.dto

import kotlinx.serialization.Serializable

/** 약속 생성(`POST api/v1/appointments`) 요청. */
@Serializable
data class AppointmentCreateRequestDto(
    val title: String,
    val dateSelectionType: String,
    val candidateDates: List<String>,
    val candidateLocations: List<CreateCandidateLocationDto>,
    val responseDeadline: String,
    val previousMembers: CreatePreviousMembersDto? = null
)

@Serializable
data class CreateCandidateLocationDto(
    val placeName: String,
    val address: String? = null,
    val lineNames: String? = null,
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class CreatePreviousMembersDto(
    val appointmentId: Long,
    val memberIds: List<Long>
)

/** 약속 생성 응답. 앱은 약속 ID와 초대 링크만 쓴다. */
@Serializable
data class AppointmentCreateResponseDto(
    val appointmentId: Long,
    val title: String? = null,
    val inviteCode: String? = null,
    val inviteUrl: String? = null
)
