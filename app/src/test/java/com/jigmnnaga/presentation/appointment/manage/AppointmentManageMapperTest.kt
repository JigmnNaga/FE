package com.jigmnnaga.presentation.appointment.manage

import com.jigmnnaga.domain.model.AppointmentAttendanceStatus
import com.jigmnnaga.domain.model.AppointmentDateSelectionType
import com.jigmnnaga.domain.model.AppointmentDetail
import com.jigmnnaga.domain.model.AppointmentLocation as DetailLocation
import com.jigmnnaga.domain.model.CandidateLocationChange
import com.jigmnnaga.domain.model.NewCandidateLocation
import com.jigmnnaga.presentation.appointment.create.AppointmentDate
import com.jigmnnaga.presentation.appointment.create.AppointmentLocation
import com.jigmnnaga.presentation.appointment.create.AppointmentScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppointmentManageMapperTest {

    private fun detail(
        type: AppointmentDateSelectionType,
        dates: List<String>,
        locations: List<DetailLocation> = emptyList()
    ) = AppointmentDetail(
        appointmentId = 7L,
        title = "스터디",
        canceled = false,
        dateSelectionType = type,
        candidateDates = dates,
        candidateLocations = locations,
        responseDeadline = null,
        inviteUrl = null,
        confirmed = false,
        confirmedDate = null,
        confirmedTime = null,
        confirmedLocation = null,
        detailPlace = null,
        myRole = null,
        myAttendanceStatus = AppointmentAttendanceStatus.PENDING,
        myNotificationEnabled = false,
        myTravelMode = null,
        joinedCount = 1,
        scheduleSubmittedCount = 0,
        members = emptyList()
    )

    @Test
    fun `하루와 날짜별과 알 수 없는 선택 방식을 편집값으로 바꾼다`() {
        val single = detail(AppointmentDateSelectionType.SINGLE, listOf("2026-11-20")).toManageForm()
        assertEquals(AppointmentScheduleType.SINGLE_DAY, single.scheduleType)
        assertEquals(AppointmentDate(2026, 11, 20), single.selectedStartDate)
        assertNull(single.selectedEndDate)

        val multiple = detail(
            AppointmentDateSelectionType.MULTIPLE,
            listOf("2026-11-22", "2026-11-20", "2026-11-20")
        ).toManageForm()
        assertEquals(AppointmentScheduleType.SPECIFIC_DATES, multiple.scheduleType)
        assertEquals(listOf(AppointmentDate(2026, 11, 20), AppointmentDate(2026, 11, 22)), multiple.selectedDates)
        assertNull(multiple.selectedStartDate)
        assertEquals(AppointmentDate(2026, 11, 1), multiple.displayedMonth)

        val other = detail(AppointmentDateSelectionType.OTHER, emptyList()).toManageForm()
        assertNull(other.scheduleType)
    }

    @Test
    fun `잘못된 날짜 문자열은 건너뛴다`() {
        val form = detail(AppointmentDateSelectionType.MULTIPLE, listOf("abc", "2026-13-01", "2026-11-20"))
            .toManageForm()

        assertEquals(listOf(AppointmentDate(2026, 11, 20)), form.selectedDates)
        assertNull(parseAppointmentDate("2026-11"))
        assertNull(parseDeadlineDate(null))
        assertEquals(AppointmentDate(2026, 11, 19), parseDeadlineDate("2026-11-19T23:59:59"))
    }

    @Test
    fun `후보 장소 ID는 화면의 장소 값과 짝지어 둔다`() {
        val seoul = DetailLocation(5L, "서울역", null, null, null, null)
        val detail = detail(AppointmentDateSelectionType.SINGLE, listOf("2026-11-20"), listOf(seoul))

        val ids = detail.existingLocationIds()

        val form = AppointmentLocation(name = "서울역", address = "")
        assertEquals(mapOf(form to 5L), ids)
        assertEquals(listOf(form), detail.toManageForm().selectedLocations)
    }

    @Test
    fun `장소 변경 요청은 기존 장소와 새 장소를 나누고 위치 없는 새 장소는 만들 수 없다`() {
        val kept = AppointmentLocation("강남역", "2호선 · 서울", "2호선", "서울", 37.0, 127.0)
        val added = AppointmentLocation("잠실역", "", "2호선", "송파", 37.5, 127.1)
        val ids = mapOf(kept to 3L)

        assertEquals(
            CandidateLocationChange(
                keepLocationIds = listOf(3L),
                newLocations = listOf(NewCandidateLocation("잠실역", "송파", "2호선", 37.5, 127.1))
            ),
            toCandidateLocationChange(listOf(kept, added, added), ids)
        )
        assertNull(toCandidateLocationChange(listOf(AppointmentLocation("어딘가", "")), ids))
    }
}
