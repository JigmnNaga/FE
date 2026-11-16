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

/**
 * 월별 캘린더 조회(`GET api/v1/appointments/me/calendar`) 응답의 날짜별 항목.
 * - [date]: `yyyy-MM-dd`
 * - [appointments]: 해당 날짜의 약속 목록 (확정 시간 오름차순)
 */
@Serializable
data class CalendarDayResponseDto(
    val date: String,
    val appointments: List<MyAppointmentResponseDto>
)

/**
 * 월별 캘린더 조회 응답 최상위 DTO.
 * 약속이 없는 날짜는 [days]에 포함되지 않는다.
 * 약속이 없는 월은 `days = []`를 반환한다.
 */
@Serializable
data class CalendarResponseDto(
    val days: List<CalendarDayResponseDto>
)
