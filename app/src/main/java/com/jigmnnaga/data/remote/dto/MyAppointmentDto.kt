package com.jigmnnaga.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * 내 약속 목록·다음 약속 조회(`GET api/v1/appointments/me`, `GET api/v1/appointments/me/next`) 응답.
 *
 * 날짜는 서버가 보낸 문자열 그대로 받는다.
 * - [confirmedDate]: `yyyy-MM-dd` (미확정이면 null)
 * - [confirmedTime]: `HH:mm` (미확정이면 null)
 * - [candidateStartDate], [candidateEndDate]: `yyyy-MM-dd` (항상 존재)
 */
@Serializable
data class MyAppointmentResponseDto(
    val appointmentId: Long,
    val title: String,
    val canceled: Boolean = false,
    val confirmed: Boolean = false,
    val confirmedDate: String? = null,
    val confirmedTime: String? = null,
    val confirmedLocationName: String? = null,
    val detailPlaceName: String? = null,
    val candidateStartDate: String? = null,
    val candidateEndDate: String? = null,
    val memberCount: Int = 0,
    val unreadMessageCount: Int = 0
)
