package com.jigmnnaga.data.remote.dto

import kotlinx.serialization.Serializable

/** 약속 상세 조회(`GET api/v1/appointments/{appointmentId}`) 응답. 날짜·시간은 서버가 보낸 문자열 그대로 받는다. */
@Serializable
data class AppointmentDetailDto(
    val appointmentId: Long,
    val title: String,
    val canceled: Boolean = false,
    val dateSelectionType: String? = null,
    val candidateDates: List<String> = emptyList(),
    val candidateLocations: List<AppointmentLocationDto> = emptyList(),
    val responseDeadline: String? = null,
    val inviteUrl: String? = null,
    val confirmed: Boolean = false,
    val confirmedDate: String? = null,
    val confirmedTime: String? = null,
    val confirmedLocation: AppointmentLocationDto? = null,
    val detailPlace: AppointmentDetailPlaceDto? = null,
    val myRole: String? = null,
    val myAttendanceStatus: String? = null,
    val myNotificationEnabled: Boolean = false,
    val myTravelMode: String? = null,
    val joinedCount: Int = 0,
    val scheduleSubmittedCount: Int = 0,
    val members: List<AppointmentMemberDto> = emptyList()
)

@Serializable
data class AppointmentLocationDto(
    val locationId: Long? = null,
    val placeName: String? = null,
    val address: String? = null,
    val lineNames: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class AppointmentDetailPlaceDto(
    val placeName: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class AppointmentMemberDto(
    val memberId: Long,
    val nickname: String,
    val profileImageUrl: String? = null,
    val role: String? = null,
    val scheduleSubmitted: Boolean = false,
    val attendanceStatus: String? = null,
    val travelMode: String? = null
)
