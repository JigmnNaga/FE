package com.jigmnnaga.presentation.appointment.create

import com.jigmnnaga.domain.model.NewAppointmentDateType
import com.jigmnnaga.domain.model.NewCandidateLocation
import com.jigmnnaga.domain.model.PreviousMembers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AppointmentCreateRequestMapperTest {

    /** 2026-11-14 12:00:00 (한국 시간) */
    private val now: Long = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
        clear()
        set(2026, 10, 14, 12, 0, 0)
    }.timeInMillis

    private val gangnam = AppointmentLocation(
        name = "강남역",
        address = "2호선, 신분당선 · 서울특별시 강남구",
        lineNames = "2호선, 신분당선",
        roadAddress = "서울특별시 강남구",
        latitude = 37.49,
        longitude = 127.02
    )

    private fun draft(
        scheduleType: AppointmentScheduleType = AppointmentScheduleType.SINGLE_DAY,
        startDate: AppointmentDate? = AppointmentDate(2026, 12, 5),
        endDate: AppointmentDate? = null,
        specificDates: List<AppointmentDate> = emptyList(),
        locations: List<AppointmentLocation> = listOf(gangnam),
        deadlineChoice: AppointmentDeadlineChoice = AppointmentDeadlineChoice.THREE_DAYS,
        customDeadline: AppointmentDate? = null
    ) = AppointmentCreateDraft(
        name = "  저녁 모임 ",
        scheduleType = scheduleType,
        startDate = startDate,
        endDate = endDate,
        specificDates = specificDates,
        locations = locations,
        deadlineChoice = deadlineChoice,
        customDeadline = customDeadline
    )

    @Test
    fun `하루 선택은 SINGLE로 보내고 제목 공백을 지운다`() {
        val request = draft().toNewAppointment(now)!!

        assertEquals("저녁 모임", request.title)
        assertEquals(NewAppointmentDateType.SINGLE, request.dateType)
        assertEquals(listOf("2026-12-05"), request.candidateDates)
        assertNull(request.previousMembers)
    }

    @Test
    fun `기간 선택은 시작일부터 종료일까지 하루씩 펼쳐 RANGE로 보낸다`() {
        val request = draft(
            scheduleType = AppointmentScheduleType.DATE_RANGE,
            startDate = AppointmentDate(2026, 11, 29),
            endDate = AppointmentDate(2026, 12, 2)
        ).toNewAppointment(now)!!

        assertEquals(NewAppointmentDateType.RANGE, request.dateType)
        assertEquals(listOf("2026-11-29", "2026-11-30", "2026-12-01", "2026-12-02"), request.candidateDates)
    }

    @Test
    fun `기간이 해를 넘어도 날짜를 이어서 만든다`() {
        val request = draft(
            scheduleType = AppointmentScheduleType.DATE_RANGE,
            startDate = AppointmentDate(2026, 12, 30),
            endDate = AppointmentDate(2027, 1, 1)
        ).toNewAppointment(now)!!

        assertEquals(listOf("2026-12-30", "2026-12-31", "2027-01-01"), request.candidateDates)
    }

    @Test
    fun `종료일이 시작일보다 늦지 않거나 기간이 너무 길면 요청을 만들지 않는다`() {
        val sameDay = draft(
            scheduleType = AppointmentScheduleType.DATE_RANGE,
            startDate = AppointmentDate(2026, 12, 5),
            endDate = AppointmentDate(2026, 12, 5)
        )
        val tooLong = draft(
            scheduleType = AppointmentScheduleType.DATE_RANGE,
            startDate = AppointmentDate(2026, 12, 1),
            endDate = AppointmentDate(2028, 12, 1)
        )
        val noEnd = draft(scheduleType = AppointmentScheduleType.DATE_RANGE, endDate = null)

        assertNull(sameDay.toNewAppointment(now))
        assertNull(tooLong.toNewAppointment(now))
        assertNull(noEnd.toNewAppointment(now))
    }

    @Test
    fun `날짜별 선택은 두 개 이상이면 MULTIPLE 하나면 SINGLE로 보낸다`() {
        val multiple = draft(
            scheduleType = AppointmentScheduleType.SPECIFIC_DATES,
            startDate = null,
            specificDates = listOf(AppointmentDate(2026, 12, 9), AppointmentDate(2026, 12, 5))
        ).toNewAppointment(now)!!
        val one = draft(
            scheduleType = AppointmentScheduleType.SPECIFIC_DATES,
            startDate = null,
            specificDates = listOf(AppointmentDate(2026, 12, 5))
        ).toNewAppointment(now)!!
        val none = draft(
            scheduleType = AppointmentScheduleType.SPECIFIC_DATES,
            startDate = null,
            specificDates = emptyList()
        )

        assertEquals(NewAppointmentDateType.MULTIPLE, multiple.dateType)
        assertEquals(listOf("2026-12-05", "2026-12-09"), multiple.candidateDates)
        assertEquals(NewAppointmentDateType.SINGLE, one.dateType)
        assertEquals(listOf("2026-12-05"), one.candidateDates)
        assertNull(none.toNewAppointment(now))
    }

    @Test
    fun `응답 기한은 선택한 기간만큼 지금부터 더한 한국 시간이다`() {
        assertEquals(
            "2026-11-15T12:00:00",
            draft(deadlineChoice = AppointmentDeadlineChoice.ONE_DAY).toNewAppointment(now)!!.responseDeadline
        )
        assertEquals(
            "2026-11-17T12:00:00",
            draft(deadlineChoice = AppointmentDeadlineChoice.THREE_DAYS).toNewAppointment(now)!!.responseDeadline
        )
        assertEquals(
            "2026-11-21T12:00:00",
            draft(deadlineChoice = AppointmentDeadlineChoice.ONE_WEEK).toNewAppointment(now)!!.responseDeadline
        )
    }

    @Test
    fun `직접 고른 응답 기한은 그날 23시 59분 59초이고 날짜가 없으면 요청을 만들지 않는다`() {
        val custom = draft(
            deadlineChoice = AppointmentDeadlineChoice.CUSTOM_DATE,
            customDeadline = AppointmentDate(2026, 12, 1)
        ).toNewAppointment(now)!!
        val missing = draft(deadlineChoice = AppointmentDeadlineChoice.CUSTOM_DATE, customDeadline = null)

        assertEquals("2026-12-01T23:59:59", custom.responseDeadline)
        assertNull(missing.toNewAppointment(now))
    }

    @Test
    fun `후보 장소는 이름 주소 노선 위치로 변환한다`() {
        val request = draft(locations = listOf(gangnam)).toNewAppointment(now)!!

        assertEquals(
            listOf(NewCandidateLocation("강남역", "서울특별시 강남구", "2호선, 신분당선", 37.49, 127.02)),
            request.candidateLocations
        )
    }

    @Test
    fun `위치가 없는 장소가 하나라도 있으면 요청을 만들지 않는다`() {
        val noLocation = AppointmentLocation("성수역", "2호선 · 서울")

        assertNull(draft(locations = listOf(gangnam, noLocation)).toNewAppointment(now))
        assertNull(
            draft(locations = listOf(gangnam.copy(longitude = null))).toNewAppointment(now)
        )
    }

    @Test
    fun `이전 약속 멤버는 그대로 담는다`() {
        val members = PreviousMembers(4L, listOf(2L, 3L))

        assertEquals(members, draft().toNewAppointment(now, members)!!.previousMembers)
    }
}
