package com.jigmnnaga.presentation.appointment

import com.jigmnnaga.domain.model.AppointmentAttendanceStatus
import com.jigmnnaga.domain.model.AppointmentDateSelectionType
import com.jigmnnaga.domain.model.AppointmentDetail
import com.jigmnnaga.domain.model.AppointmentMemberRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentRoomRoutingTest {

    private fun detail(role: AppointmentMemberRole?) = AppointmentDetail(
        appointmentId = 7L,
        title = "스터디",
        canceled = false,
        dateSelectionType = AppointmentDateSelectionType.SINGLE,
        candidateDates = emptyList(),
        candidateLocations = emptyList(),
        responseDeadline = null,
        inviteUrl = null,
        confirmed = false,
        confirmedDate = null,
        confirmedTime = null,
        confirmedLocation = null,
        detailPlace = null,
        myRole = role,
        myAttendanceStatus = AppointmentAttendanceStatus.PENDING,
        myNotificationEnabled = true,
        myTravelMode = null,
        joinedCount = 1,
        scheduleSubmittedCount = 0,
        members = emptyList()
    )

    @Test
    fun `방장일 때만 방장 약속방을 연다`() {
        assertTrue(detail(AppointmentMemberRole.HOST).opensHostRoom())
        assertFalse(detail(AppointmentMemberRole.MEMBER).opensHostRoom())
        assertFalse(detail(AppointmentMemberRole.OTHER).opensHostRoom())
        assertFalse(detail(null).opensHostRoom())
    }

    @Test
    fun `문자열 약속 ID는 숫자일 때만 약속방 목적지가 된다`() {
        assertEquals(AppointmentRoomDestination(12L), "12".toAppointmentRoomDestination())
        assertEquals(AppointmentRoomDestination(12L), " 12 ".toAppointmentRoomDestination())
        assertNull("abc".toAppointmentRoomDestination())
        assertNull("".toAppointmentRoomDestination())
    }
}
