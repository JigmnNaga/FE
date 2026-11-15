package com.jigmnnaga.presentation.appointment.schedule

import org.junit.Assert.assertEquals
import org.junit.Test

class AppointmentDateTimeUiStateTest {
    @Test
    fun `month exposes calendar dimensions`() {
        val september = AppointmentCalendarMonth(2026, 9)

        assertEquals(30, september.dayCount())
        assertEquals(2, september.firstWeekdayOffset())
        assertEquals("2026년 9월", september.label())
    }

    @Test
    fun `time label is zero padded`() {
        assertEquals("09:05", AppointmentTimeValue(9, 5).label())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid month is rejected`() {
        AppointmentCalendarMonth(2026, 13)
    }

    @Test
    fun `member responses are grouped by status`() {
        val state = AppointmentDateTimeUiState(
            appointmentTitle = "중간고사 카공 모임",
            displayedMonth = AppointmentCalendarMonth(2026, 9),
            memberResponses = listOf(
                MemberResponseUiState("1", "태양", MemberResponseStatus.COMPLETED),
                MemberResponseUiState("2", "나", MemberResponseStatus.PENDING, isMe = true),
                MemberResponseUiState("3", "하은", MemberResponseStatus.PENDING)
            )
        )

        assertEquals(listOf("태양"), state.completedMembers.map { it.name })
        assertEquals(listOf("나", "하은"), state.pendingMembers.map { it.name })
        assertEquals("태", state.completedMembers.first().initial)
    }

    @Test
    fun isoStringsRoundTripAndRejectInvalidValues() {
        val date = AppointmentDateValue(2026, 3, 5)
        assertEquals("2026-03-05", date.toIsoString())
        assertEquals(date, AppointmentDateValue.parseIso("2026-03-05"))
        assertEquals(null, AppointmentDateValue.parseIso("2026-13-05"))
        assertEquals(null, AppointmentDateValue.parseIso("2026-03"))
        assertEquals(null, AppointmentDateValue.parseIso("abc"))

        assertEquals(AppointmentTimeValue(9, 5), AppointmentTimeValue.parse("09:05"))
        assertEquals(null, AppointmentTimeValue.parse("25:00"))
        assertEquals(null, AppointmentTimeValue.parse("9"))
    }
}
