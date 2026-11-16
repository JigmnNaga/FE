package com.jigmnnaga.presentation.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarUiStateTest {

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private fun appointment(
        year: Int = 2026,
        month: Int = 9,
        day: Int = 6,
        time: String = "15:00"
    ) = MockCalendarAppointment(
        id = "$year-$month-$day-$time",
        title = "약속",
        year = year,
        month = month,
        day = day,
        dayLabel = "일",
        time = time,
        location = "장소",
        participants = emptyList()
    )

    private fun state(
        year: Int = 2026,
        month: Int = 9,
        appointments: List<MockCalendarAppointment> = emptyList(),
        selectedDate: CalendarDate? = null
    ) = CalendarUiState(
        displayMonth = CalendarMonth(year, month),
        today = CalendarDate(year, month, 1),
        appointments = appointments,
        selectedDate = selectedDate
    )

    // ── appointmentDays ───────────────────────────────────────────────────────

    @Test
    fun `appointmentDays는 현재 월의 약속 날짜 집합을 반환한다`() {
        val s = state(
            appointments = listOf(
                appointment(day = 6),
                appointment(day = 12),
                appointment(day = 20)
            )
        )

        assertEquals(setOf(6, 12, 20), s.appointmentDays)
    }

    @Test
    fun `appointmentDays는 selectedDate와 무관하게 모든 약속 날짜를 반환한다`() {
        val s = state(
            appointments = listOf(appointment(day = 6), appointment(day = 12)),
            selectedDate = CalendarDate(2026, 9, 6)
        )

        // selectedDate가 6일이어도 appointmentDays는 6, 12 모두 포함
        assertEquals(setOf(6, 12), s.appointmentDays)
    }

    @Test
    fun `다른 연월 약속은 appointmentDays에 포함되지 않는다`() {
        val s = state(
            appointments = listOf(
                appointment(year = 2026, month = 9, day = 6),
                appointment(year = 2026, month = 10, day = 1),  // 다음 달
                appointment(year = 2025, month = 9, day = 6)    // 작년
            )
        )

        assertEquals(setOf(6), s.appointmentDays)
    }

    @Test
    fun `약속이 없으면 appointmentDays가 빈 집합이다`() {
        assertTrue(state().appointmentDays.isEmpty())
    }

    // ── monthAppointments ─────────────────────────────────────────────────────

    @Test
    fun `selectedDate가 null이면 현재 월 전체 약속을 반환한다`() {
        val s = state(
            appointments = listOf(appointment(day = 6), appointment(day = 12))
        )

        assertEquals(2, s.monthAppointments.size)
    }

    @Test
    fun `selectedDate가 설정되면 해당 날짜의 약속만 반환한다`() {
        val s = state(
            appointments = listOf(appointment(day = 6), appointment(day = 12)),
            selectedDate = CalendarDate(2026, 9, 6)
        )

        val result = s.monthAppointments
        assertEquals(1, result.size)
        assertEquals(6, result.first().day)
    }

    @Test
    fun `monthAppointments는 날짜 오름차순으로 정렬된다`() {
        val s = state(
            appointments = listOf(
                appointment(day = 20),
                appointment(day = 6),
                appointment(day = 12)
            )
        )

        val days = s.monthAppointments.map { it.day }
        assertEquals(listOf(6, 12, 20), days)
    }

    @Test
    fun `같은 날짜 약속은 시간 오름차순으로 정렬된다`() {
        val s = state(
            appointments = listOf(
                appointment(day = 6, time = "18:00"),
                appointment(day = 6, time = "10:00"),
                appointment(day = 6, time = "14:00")
            )
        )

        val times = s.monthAppointments.map { it.time }
        assertEquals(listOf("10:00", "14:00", "18:00"), times)
    }

    @Test
    fun `다른 연월 약속은 monthAppointments에 포함되지 않는다`() {
        val s = state(
            appointments = listOf(
                appointment(year = 2026, month = 9, day = 6),
                appointment(year = 2026, month = 10, day = 1)
            )
        )

        assertEquals(1, s.monthAppointments.size)
        assertEquals(6, s.monthAppointments.first().day)
    }

    // ── CalendarMonth.prev / next ─────────────────────────────────────────────

    @Test
    fun `CalendarMonth prev는 이전 달을 반환한다`() {
        val prev = CalendarMonth(2026, 9).prev()
        assertEquals(2026, prev.year)
        assertEquals(8, prev.month)
    }

    @Test
    fun `CalendarMonth next는 다음 달을 반환한다`() {
        val next = CalendarMonth(2026, 9).next()
        assertEquals(2026, next.year)
        assertEquals(10, next.month)
    }

    @Test
    fun `1월의 prev는 전년도 12월이다`() {
        val prev = CalendarMonth(2026, 1).prev()
        assertEquals(2025, prev.year)
        assertEquals(12, prev.month)
    }

    @Test
    fun `12월의 next는 다음년도 1월이다`() {
        val next = CalendarMonth(2026, 12).next()
        assertEquals(2027, next.year)
        assertEquals(1, next.month)
    }

    @Test
    fun `CalendarMonth dayCount는 양수이다`() {
        assertTrue(CalendarMonth(2026, 9).dayCount() > 0)
    }

    @Test
    fun `CalendarMonth firstDayOfWeekOffset은 0부터 6 사이이다`() {
        val offset = CalendarMonth(2026, 9).firstDayOfWeekOffset()
        assertTrue(offset in 0..6)
    }

    // ── CalendarUiState 필드 기본값 ───────────────────────────────────────────

    @Test
    fun `CalendarUiState 기본 isLoading은 false이다`() {
        assertFalse(state().isLoading)
    }

    @Test
    fun `CalendarUiState 기본 error는 null이다`() {
        assertEquals(null, state().error)
    }

    @Test
    fun `CalendarUiState 기본 loginRequired는 false이다`() {
        assertFalse(state().loginRequired)
    }

    @Test
    fun `CalendarUiState 기본 selectedDate는 null이다`() {
        assertEquals(null, state().selectedDate)
    }
}
