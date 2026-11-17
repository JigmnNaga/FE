package com.jigmnnaga.presentation.calendar

import com.jigmnnaga.presentation.common.error.ErrorType
import java.util.Calendar

// ─────────────────────────────────────────
// 날짜 모델 (java.util.Calendar 기반, minSdk 24 대응)
// ─────────────────────────────────────────

data class CalendarMonth(val year: Int, val month: Int) {
    init { require(month in 1..12) }

    fun dayCount(): Int = Calendar.getInstance().apply {
        set(year, month - 1, 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)

    /** 1일의 요일 offset: 0 = 일요일, 6 = 토요일 */
    fun firstDayOfWeekOffset(): Int = Calendar.getInstance().apply {
        set(year, month - 1, 1)
    }.get(Calendar.DAY_OF_WEEK) - 1

    fun prev(): CalendarMonth = if (month == 1) CalendarMonth(year - 1, 12) else CalendarMonth(year, month - 1)
    fun next(): CalendarMonth = if (month == 12) CalendarMonth(year + 1, 1) else CalendarMonth(year, month + 1)
}

data class CalendarDate(val year: Int, val month: Int, val day: Int)

// ─────────────────────────────────────────
// UI 모델
// ─────────────────────────────────────────

data class CalendarParticipant(
    val initial: String,
    val isMe: Boolean = false
)

/**
 * 캘린더 화면에서 표시할 약속 UI 모델.
 * - CalendarViewModel이 HomeAppointment를 변환해 채운다.
 * - [participants]는 캘린더 API에서 개별 참여자 정보를 제공하지 않으므로 항상 `emptyList()`다.
 * - [location]은 장소 정보가 없으면 "장소 미정"으로 채운다.
 */
data class MockCalendarAppointment(
    val id: String,
    val title: String,
    val year: Int,
    val month: Int,
    val day: Int,
    /** 날짜 박스 상단 레이블. 오늘이면 "TODAY", 아니면 요일(예: "토") */
    val dayLabel: String,
    val time: String,
    val location: String,
    val participants: List<CalendarParticipant>,
    val isToday: Boolean = false
)

/** 캘린더 조회 오류. */
data class CalendarError(val retryable: Boolean, val isNetworkError: Boolean = false)

fun CalendarError.toErrorType(): ErrorType =
    if (isNetworkError) ErrorType.Network else ErrorType.Service

// ─────────────────────────────────────────
// UI 상태
// ─────────────────────────────────────────

/**
 * 캘린더 화면 UI 상태.
 *
 * - [isLoading]: API 조회 진행 중
 * - [error]: 조회 오류 (null이면 정상)
 * - [loginRequired]: 인증 필요 — Route에서 로그인 화면으로 이동 후 consumeLoginRequired 호출
 * - [selectedDate]: 선택된 날짜 (null이면 전체 월 표시)
 */
data class CalendarUiState(
    val displayMonth: CalendarMonth,
    val today: CalendarDate,
    val appointments: List<MockCalendarAppointment>,
    val isLoading: Boolean = false,
    val error: CalendarError? = null,
    val loginRequired: Boolean = false,
    val selectedDate: CalendarDate? = null
) {
    /**
     * 현재 표시 중인 월의 약속 목록 (날짜·시간 오름차순).
     * [selectedDate]가 설정되어 있으면 해당 날짜의 약속만 반환한다.
     */
    val monthAppointments: List<MockCalendarAppointment>
        get() {
            val base = appointments
                .filter { it.year == displayMonth.year && it.month == displayMonth.month }
                .sortedWith(compareBy({ it.day }, { it.time }))
            return if (selectedDate != null) {
                base.filter { it.day == selectedDate.day }
            } else {
                base
            }
        }

    /**
     * 현재 표시 월에서 약속이 있는 날짜 집합.
     * [selectedDate] 여부와 무관하게 모든 약속 날짜를 반환한다 (캘린더 그리드 하이라이트용).
     */
    val appointmentDays: Set<Int>
        get() = appointments
            .filter { it.year == displayMonth.year && it.month == displayMonth.month }
            .map { it.day }
            .toSet()

    companion object {
        private fun todayDate(): CalendarDate = Calendar.getInstance().let {
            CalendarDate(
                year = it.get(Calendar.YEAR),
                month = it.get(Calendar.MONTH) + 1,
                day = it.get(Calendar.DAY_OF_MONTH)
            )
        }

        private val mockParticipants1 = listOf(
            CalendarParticipant("나", isMe = true),
            CalendarParticipant("지"),
            CalendarParticipant("민")
        )

        private val mockParticipants2 = listOf(
            CalendarParticipant("나", isMe = true),
            CalendarParticipant("채"),
            CalendarParticipant("도")
        )

        // Preview용 목업 약속 데이터 (Figma HOM-002-02 기준)
        private val mockAppointments = listOf(
            MockCalendarAppointment(
                id = "cal_1",
                title = "스터디 모임",
                year = 2026, month = 9, day = 6,
                dayLabel = "TODAY",
                time = "오후 3:00",
                location = "강남역 카공카페",
                participants = mockParticipants1,
                isToday = true
            ),
            MockCalendarAppointment(
                id = "cal_2",
                title = "주말 영화",
                year = 2026, month = 9, day = 12,
                dayLabel = "토",
                time = "오후 6:00",
                location = "CGV 신촌",
                participants = mockParticipants2,
                isToday = false
            )
        )

        /** Preview: 빈 상태 (약속 없음) */
        val PreviewEmpty = CalendarUiState(
            displayMonth = CalendarMonth(2026, 9),
            today = CalendarDate(2026, 9, 1),
            appointments = emptyList()
        )

        /** Preview: 약속 목록 있음 */
        val PreviewWithAppointments = CalendarUiState(
            displayMonth = CalendarMonth(2026, 9),
            today = CalendarDate(2026, 9, 6),
            appointments = mockAppointments
        )

        /** Preview: 오늘이 월말에 위치한 상태 */
        val PreviewTodayAtEnd = CalendarUiState(
            displayMonth = CalendarMonth(2026, 9),
            today = CalendarDate(2026, 9, 29),
            appointments = mockAppointments
        )

        /** Preview: 약속 없는 월 */
        val PreviewEmptyMonth = CalendarUiState(
            displayMonth = CalendarMonth(2026, 10),
            today = CalendarDate(2026, 10, 1),
            appointments = emptyList()
        )

        /**
         * CalendarRoute에서 CalendarViewModel 없이 사용하는 초기 상태.
         * ViewModel 연동 전까지 목업 데이터를 표시한다.
         */
        fun initial(): CalendarUiState {
            val t = todayDate()
            return CalendarUiState(
                displayMonth = CalendarMonth(t.year, t.month),
                today = t,
                appointments = mockAppointments
            )
        }
    }
}
