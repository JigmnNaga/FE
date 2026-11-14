package com.jigmnnaga.data.remote.dto

import kotlinx.serialization.Serializable

/** 참석 응답(`PUT api/v1/appointments/{appointmentId}/attendance/me`) 요청. */
@Serializable
data class AttendanceRespondRequestDto(
    val attendanceStatus: String
)
