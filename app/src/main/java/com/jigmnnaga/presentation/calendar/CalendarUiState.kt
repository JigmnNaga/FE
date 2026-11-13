package com.jigmnnaga.presentation.calendar

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
// 목업 모델
// ─────────────────────────────────────────

data class CalendarParticipant(
    val initial: String,
    val isMe: Boolean = false
)

/**
 * 캘린더 화면에서 표시할 약속 목업 모델.
 * API 연동 시 domain 레이어의 Appointment 모델로 교체한다.
 */
data class MockCalendarAppointment(
    val id: String,
    val title: String,
    val year: Int,
    val month: Int,
    val day: Int,
    /** 날짜 박스 상단에 표시할 레이블. 오늘이면 "TODAY", 아니면 요일(예: "토") */
    val dayLabel: String,
    val time: String,
    val location: String,
    val participants: List<CalendarParticipant>,
    val isToday: Boolean = false
)

// ─────────────────────────────────────────
// UI 상태
// ─────────────────────────────────────────

/**
 * 캘린더 화면 UI 상태.
 * API 연동 시 CalendarViewModel이 이 상태를 StateFlow로 노출한다.
 *
 * API 연동 방법:
 * 1. CalendarViewModel 생성 후 CalendarRoute.kt의 remember 블록 제거
 * 2. `val uiState by viewModel.uiState.collectAsStateWithLifecycle()` 로 대체
 * 3. MockCalendarAppointment → domain 레이어의 Appointment 모델로 교체
 * 4. displayMonth 변경 시 viewModel::onPrevMonth / viewModel::onNextMonth 연결
 */
data class CalendarUiState(
    val displayMonth: CalendarMonth,
    val today: CalendarDate,
    val appointments: List<MockCalendarAppointment>
) {
    /** 현재 표시 중인 월에 해당하는 약속 목록 (날짜 오름차순) */
    val monthAppointments: List<MockCalendarAppointment>
        get() = appointments
            .filter { it.year == displayMonth.year && it.month == displayMonth.month }
            .sortedWith(compareBy({ it.day }, { it.time }))

    /** 현재 표시 월에서 약속이 있는 날짜 집합 */
    val appointmentDays: Set<Int>
        get() = monthAppointments.map { it.day }.toSet()

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

        // 목업 약속 데이터 (Figma HOM-002-02 기준)
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

        /** 실기기 사용 초기 상태 — 이번 달 목업 약속 표시 */
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
