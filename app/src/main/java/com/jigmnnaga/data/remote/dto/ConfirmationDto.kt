package com.jigmnnaga.data.remote.dto

import kotlinx.serialization.Serializable

/** 날짜·시간 확정(`PUT api/v1/appointments/{appointmentId}/confirmation/date-time`) 요청. */
@Serializable
data class ConfirmDateTimeRequestDto(
    val date: String,
    val time: String
)

/** 장소 확정(`PUT .../confirmation/location`) 요청. [detailPlace]가 null이면 세부 위치를 나중에 정한다. */
@Serializable
data class ConfirmLocationRequestDto(
    val locationId: Long,
    val detailPlace: DetailPlaceDto? = null
)

@Serializable
data class DetailPlaceDto(
    val placeName: String,
    val address: String? = null,
    val latitude: Double,
    val longitude: Double
)

/** 시간대별 가능 인원 집계(`GET .../schedules/heatmap`) 응답. 확정 화면에는 상위 후보만 쓴다. */
@Serializable
data class ScheduleHeatmapDto(
    val slotMinutes: Int = 60,
    val totalMemberCount: Int = 0,
    val topCandidates: List<TimeRangeCandidateDto> = emptyList()
)

@Serializable
data class TimeRangeCandidateDto(
    val date: String,
    val startTime: String,
    val endTime: String,
    val availableCount: Int = 0
)
