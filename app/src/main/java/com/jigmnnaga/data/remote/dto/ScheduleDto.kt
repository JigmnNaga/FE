package com.jigmnnaga.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AvailabilityDto(
    val date: String,
    val startTime: String,
    val endTime: String
)

/** 일정 제출(`PUT api/v1/appointments/{appointmentId}/schedules/me`) 요청. */
@Serializable
data class ScheduleSubmitRequestDto(
    val availabilities: List<AvailabilityDto>,
    val preferredLocationId: Long
)

/** 내 일정 조회(`GET .../schedules/me`) 응답. */
@Serializable
data class MyScheduleDto(
    val submitted: Boolean = false,
    val availabilities: List<AvailabilityDto> = emptyList(),
    val preferredLocationId: Long? = null
)

/** 일정 응답 현황 조회(`GET .../schedules`) 응답. */
@Serializable
data class ScheduleStatusDto(
    val totalCount: Int = 0,
    val submittedCount: Int = 0,
    val members: List<MemberScheduleDto> = emptyList(),
    val locationVotes: List<LocationVoteDto> = emptyList()
)

@Serializable
data class MemberScheduleDto(
    val memberId: Long,
    val nickname: String,
    val profileImageUrl: String? = null,
    val submitted: Boolean = false,
    val availabilities: List<AvailabilityDto> = emptyList(),
    val preferredLocationId: Long? = null
)

@Serializable
data class LocationVoteDto(
    val locationId: Long,
    val placeName: String? = null,
    val address: String? = null,
    val lineNames: String? = null,
    val voteCount: Int = 0,
    val topVoted: Boolean = false,
    val voters: List<ScheduleVoterDto> = emptyList()
)

@Serializable
data class ScheduleVoterDto(
    val memberId: Long,
    val nickname: String,
    val profileImageUrl: String? = null
)
