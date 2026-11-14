package com.jigmnnaga.presentation.appointment.detail

import com.jigmnnaga.domain.model.AppointmentAttendanceStatus
import com.jigmnnaga.domain.model.AppointmentDateSelectionType
import com.jigmnnaga.domain.model.AppointmentDetail
import com.jigmnnaga.domain.model.AppointmentDetailPlace
import com.jigmnnaga.domain.model.AppointmentLocation
import com.jigmnnaga.domain.model.AppointmentMember
import com.jigmnnaga.domain.model.AppointmentMemberRole
import com.jigmnnaga.presentation.appointment.existingadd.ExistingAppointmentAddPage
import com.jigmnnaga.presentation.appointment.finalconfirmation.AppointmentAttendanceChoice
import com.jigmnnaga.presentation.appointment.participant.ParticipantAppointmentStatus
import com.jigmnnaga.presentation.appointment.schedule.MemberResponseStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentDetailUiMapperTest {

    private val labels = AppointmentDetailLabels(
        scheduleBeforeConfirm = "확정 전",
        scheduleConfirmed = "확정",
        scheduleCanceled = "취소된 약속",
        scheduleSubmitted = "일정 제출",
        schedulePending = "일정 미제출",
        attending = "참석",
        notAttending = "불참",
        attendancePending = "응답 전",
        rescheduleRequested = "재조율 요청",
        placeholder = "-",
        roomSubtitle = { "${it}명 · 약속방" },
        memberSummary = { "${it}명" }
    )

    private fun member(
        id: Long,
        name: String,
        role: AppointmentMemberRole = AppointmentMemberRole.MEMBER,
        submitted: Boolean = false,
        attendance: AppointmentAttendanceStatus = AppointmentAttendanceStatus.PENDING
    ) = AppointmentMember(id, name, null, role, submitted, attendance, null)

    private fun detail(
        confirmed: Boolean = false,
        canceled: Boolean = false,
        myRole: AppointmentMemberRole? = AppointmentMemberRole.HOST,
        myAttendance: AppointmentAttendanceStatus = AppointmentAttendanceStatus.PENDING,
        confirmedDate: String? = "2026-11-20",
        confirmedTime: String? = "15:00"
    ) = AppointmentDetail(
        appointmentId = 7L,
        title = "중간고사 카공 모임",
        canceled = canceled,
        dateSelectionType = AppointmentDateSelectionType.MULTIPLE,
        candidateDates = listOf("2026-11-20"),
        candidateLocations = emptyList(),
        responseDeadline = null,
        inviteUrl = null,
        confirmed = confirmed,
        confirmedDate = confirmedDate,
        confirmedTime = confirmedTime,
        confirmedLocation = AppointmentLocation(3L, "강남역", null, null, null, null),
        detailPlace = AppointmentDetailPlace("카공카페", null, null, null),
        myRole = myRole,
        myAttendanceStatus = myAttendance,
        myNotificationEnabled = false,
        myTravelMode = null,
        joinedCount = 3,
        scheduleSubmittedCount = 2,
        members = listOf(
            member(1L, "나", AppointmentMemberRole.HOST, submitted = true, attendance = AppointmentAttendanceStatus.ATTENDING),
            member(2L, "태양", submitted = true, attendance = AppointmentAttendanceStatus.NOT_ATTENDING),
            member(3L, "하은")
        )
    )

    @Test
    fun `날짜와 시간을 한국어로 바꾼다`() {
        assertEquals("11월 20일 금요일", formatDateLabel("2026-11-20"))
        assertEquals("오후 3:00", formatTimeLabel("15:00"))
        assertEquals("오전 9:05", formatTimeLabel("09:05"))
    }

    @Test
    fun `읽을 수 없는 날짜와 시간은 null이다`() {
        assertNull(formatDateLabel(null))
        assertNull(formatDateLabel(""))
        assertNull(formatDateLabel("2026-13-40"))
        assertNull(formatDateLabel("내일"))
        assertNull(formatTimeLabel(null))
        assertNull(formatTimeLabel("25:99"))
    }

    @Test
    fun `확정된 약속만 날짜 요약이 있고 시간을 읽지 못하면 날짜만 보인다`() {
        assertEquals("11월 20일 금요일 오후 3:00", detail(confirmed = true).dateSummary())
        assertNull(detail(confirmed = false).dateSummary())
        assertEquals("11월 20일 금요일", detail(confirmed = true, confirmedTime = null).dateSummary())
        assertNull(detail(confirmed = true, confirmedDate = null).dateSummary())
    }

    @Test
    fun `장소 요약은 확정 장소와 세부 장소를 합친다`() {
        assertEquals("강남역 카공카페", detail().placeSummary())
    }

    @Test
    fun `방장 약속방 상태에 약속 정보와 참여자가 담긴다`() {
        val state = detail().toHostRoomUiState(labels)

        assertEquals("중간고사 카공 모임", state.title)
        assertEquals("3명 · 약속방", state.subtitle)
        assertEquals("확정 전", state.scheduleStatusLabel)
        assertNull(state.dateSummary)
        assertEquals(2, state.responseCount)
        assertEquals(3, state.participantCount)
        assertEquals(listOf("1", "2", "3"), state.participants.map { it.id })
        assertTrue(state.participants[0].isHost)
        assertFalse(state.participants[1].isHost)
        assertEquals("일정 제출", state.participants[0].statusLabel)
        assertEquals("일정 미제출", state.participants[2].statusLabel)
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun `확정되면 멤버 상태가 참석 응답으로 바뀌고 취소되면 취소 문구가 나온다`() {
        val confirmed = detail(confirmed = true).toHostRoomUiState(labels)
        assertEquals("확정", confirmed.scheduleStatusLabel)
        assertEquals(listOf("참석", "불참", "응답 전"), confirmed.participants.map { it.statusLabel })

        val canceled = detail(canceled = true, confirmed = true).toHostRoomUiState(labels)
        assertEquals("취소된 약속", canceled.scheduleStatusLabel)
    }

    @Test
    fun `참여자 약속방은 확정 여부로 상태가 정해지고 방장일 때만 나를 알 수 있다`() {
        val waiting = detail().toParticipantRoomUiState(labels)
        assertEquals(ParticipantAppointmentStatus.AWAITING_CONFIRMATION, waiting.appointmentStatus)
        assertEquals(listOf(true, false, false), waiting.members.map { it.isCurrentUser })

        val confirmed = detail(confirmed = true, myRole = AppointmentMemberRole.MEMBER).toParticipantRoomUiState(labels)
        assertEquals(ParticipantAppointmentStatus.CONFIRMED, confirmed.appointmentStatus)
        assertEquals("11월 20일 금요일 오후 3:00", confirmed.dateSummary)
        assertEquals("강남역 카공카페", confirmed.placeSummary)
        assertEquals(listOf(false, false, false), confirmed.members.map { it.isCurrentUser })
        assertFalse(confirmed.canReportDeparture)
    }

    @Test
    fun `확정 확인 화면에 확정 정보와 내 참석 응답이 담긴다`() {
        val state = detail(confirmed = true, myAttendance = AppointmentAttendanceStatus.ATTENDING)
            .toFinalConfirmationUiState(labels)

        assertEquals("중간고사 카공 모임", state.appointmentTitle)
        assertEquals("11월 20일 금요일", state.dateLabel)
        assertEquals("오후 3:00", state.timeLabel)
        assertEquals("강남역", state.placeName)
        assertEquals("카공카페", state.detailLocation)
        assertEquals(AppointmentAttendanceChoice.ATTENDING, state.attendanceChoice)
    }

    @Test
    fun `확정 정보가 없으면 자리표시 문구가 들어가고 응답 전이면 선택이 비어 있다`() {
        val state = detail(confirmedDate = null, confirmedTime = null).toFinalConfirmationUiState(labels)

        assertEquals("-", state.dateLabel)
        assertEquals("-", state.timeLabel)
        assertNull(state.attendanceChoice)

        val notAttending = detail(myAttendance = AppointmentAttendanceStatus.NOT_ATTENDING)
            .toFinalConfirmationUiState(labels)
        assertEquals(AppointmentAttendanceChoice.NOT_ATTENDING, notAttending.attendanceChoice)
    }

    @Test
    fun `기존 약속 추가 화면은 첫 페이지로 시작하고 나는 고를 수 없다`() {
        val state = detail(confirmed = true).toExistingAddUiState(labels)

        assertEquals(ExistingAppointmentAddPage.RESCHEDULE, state.page)
        assertEquals("3명", state.memberSummary)
        assertEquals("11월 20일 금요일 오후 3:00", state.scheduledTime)
        assertEquals("강남역 카공카페", state.scheduledPlace)
        assertEquals(listOf(true, false, false), state.members.map { it.isMe })
        assertEquals(listOf(false, true, true), state.members.map { it.isSelectable })
        assertEquals(listOf("참석", "불참", "응답 전"), state.members.map { it.statusLabel })
    }

    @Test
    fun `확정 전이면 기존 약속 추가 화면의 시간은 자리표시 문구이다`() {
        assertEquals("-", detail(confirmed = false).toExistingAddUiState(labels).scheduledTime)
    }

    @Test
    fun `날짜 선택 화면의 참여자 응답은 일정 제출 여부로 정해진다`() {
        val responses = detail().toMemberResponses()

        assertEquals(
            listOf(MemberResponseStatus.COMPLETED, MemberResponseStatus.COMPLETED, MemberResponseStatus.PENDING),
            responses.map { it.status }
        )
        assertEquals(listOf("나", "태양", "하은"), responses.map { it.name })
        assertEquals(listOf(true, false, false), responses.map { it.isMe })
    }
}
