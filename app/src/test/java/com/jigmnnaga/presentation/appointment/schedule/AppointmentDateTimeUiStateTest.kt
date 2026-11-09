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
}
