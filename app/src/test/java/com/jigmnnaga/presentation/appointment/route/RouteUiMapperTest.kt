package com.jigmnnaga.presentation.appointment.route

import com.jigmnnaga.domain.model.AppointmentAttendanceStatus
import com.jigmnnaga.domain.model.AppointmentDateSelectionType
import com.jigmnnaga.domain.model.AppointmentDetail
import com.jigmnnaga.domain.model.AppointmentDetailPlace
import com.jigmnnaga.domain.model.AppointmentLocation
import com.jigmnnaga.domain.model.AppointmentMember
import com.jigmnnaga.domain.model.AppointmentMemberRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteUiMapperTest {

    private val labels = RouteMemberLabels(
        notStarted = "미출발",
        waiting = "아직 출발 전이에요",
        setRoute = "이동 경로를 설정해주세요."
    )

    private fun member(id: Long, name: String, role: AppointmentMemberRole = AppointmentMemberRole.MEMBER) =
        AppointmentMember(id, name, null, role, false, AppointmentAttendanceStatus.PENDING, null)

    private fun detail(myRole: AppointmentMemberRole? = AppointmentMemberRole.HOST) = AppointmentDetail(
        appointmentId = 7L,
        title = "중간고사 카공 모임",
        canceled = false,
        dateSelectionType = AppointmentDateSelectionType.MULTIPLE,
        candidateDates = listOf("2026-11-20"),
        candidateLocations = emptyList(),
        responseDeadline = null,
        inviteUrl = null,
        confirmed = true,
        confirmedDate = "2026-11-20",
        confirmedTime = "15:00",
        confirmedLocation = AppointmentLocation(3L, "강남역", null, null, null, null),
        detailPlace = AppointmentDetailPlace("카공카페", null, null, null),
        myRole = myRole,
        myAttendanceStatus = AppointmentAttendanceStatus.PENDING,
        myNotificationEnabled = false,
        myTravelMode = null,
        joinedCount = 3,
        scheduleSubmittedCount = 2,
        members = listOf(
            member(1L, "나", AppointmentMemberRole.HOST),
            member(2L, "태양"),
            member(3L, "하은")
        )
    )

    @Test
    fun `제목과 멤버 요약과 약속 장소는 약속 상세 값으로 채운다`() {
        val state = detail().toRouteUiState("3명", labels, myMemberId = 1L)

        assertEquals("중간고사 카공 모임", state.appointmentTitle)
        assertEquals("3명", state.memberSummary)
        assertEquals("강남역 카공카페", state.destinationName)
        assertEquals(RouteStage.OVERVIEW, state.stage)
    }

    @Test
    fun `내 멤버 ID가 같은 멤버가 나이고 맨 앞에 온다`() {
        val state = detail(AppointmentMemberRole.MEMBER).toRouteUiState("3명", labels, myMemberId = 3L)

        assertEquals(listOf("3", "1", "2"), state.members.map { it.id })
        assertEquals(listOf(true, false, false), state.members.map { it.isMe })
    }

    @Test
    fun `내 멤버 ID를 모르면 방장일 때만 방장이 나이다`() {
        val asHost = detail(AppointmentMemberRole.HOST).toRouteUiState("3명", labels, myMemberId = null)
        assertEquals("1", asHost.members.single { it.isMe }.id)

        val asMember = detail(AppointmentMemberRole.MEMBER).toRouteUiState("3명", labels, myMemberId = null)
        assertTrue(asMember.members.none { it.isMe })
    }

    @Test
    fun `나는 경로 설정 안내를, 다른 멤버는 출발 전 안내를 보여 준다`() {
        val state = detail().toRouteUiState("3명", labels, myMemberId = 1L)

        val me = state.members.first { it.isMe }
        assertEquals("이동 경로를 설정해주세요.", me.description)
        assertEquals("미출발", me.statusLabel)
        val other = state.members.first { !it.isMe }
        assertEquals("아직 출발 전이에요", other.description)
        assertFalse(other.isMe)
        assertTrue(state.members.all { it.status == RouteMemberStatus.NOT_STARTED })
    }

    @Test
    fun `경로 선택지와 이동 정보는 출발 위치를 고른 뒤 서버에서 받으므로 처음에는 비어 있다`() {
        val state = detail().toRouteUiState("3명", labels, myMemberId = 1L)

        assertTrue(state.options.isEmpty())
        assertEquals(null, state.moving)
        assertEquals("", state.destinationEstimateTitle)
    }

    @Test
    fun `약속 시각은 확정된 시간을 오전 오후 문구로 보여 준다`() {
        val state = detail().toRouteUiState("3명", labels, myMemberId = 1L)

        assertEquals(RouteTime.formatClockText("15:00"), state.meetingTimeLabel)
        assertTrue(state.meetingTimeLabel.endsWith("3:00"))
    }
}
