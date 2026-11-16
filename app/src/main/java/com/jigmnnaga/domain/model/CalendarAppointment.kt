package com.jigmnnaga.domain.model

/**
 * 캘린더 화면의 날짜별 약속 그룹.
 * - [date]: 서버 문자열 그대로 보관 (`yyyy-MM-dd`)
 * - [appointments]: 해당 날짜의 약속 목록 (서버 정렬 그대로 유지 — 확정 시간 오름차순)
 */
data class CalendarDay(
    val date: String,
    val appointments: List<HomeAppointment>
)

/** 월별 캘린더 조회 결과. */
sealed interface CalendarResult {
    /** 조회 성공. [days]가 비어 있으면 해당 월에 약속 없음. */
    data class Success(val days: List<CalendarDay>) : CalendarResult
    data object LoginRequired : CalendarResult
    data class Failure(val retryable: Boolean) : CalendarResult
}
