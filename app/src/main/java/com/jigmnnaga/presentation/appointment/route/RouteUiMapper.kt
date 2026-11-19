package com.jigmnnaga.presentation.appointment.route

import com.jigmnnaga.domain.model.AppointmentDetail
import com.jigmnnaga.domain.model.AppointmentMemberRole
import com.jigmnnaga.domain.model.AppointmentTravelMode
import com.jigmnnaga.presentation.appointment.detail.placeSummary

/** 이동 경로 화면 멤버 카드에 보여 줄 문구. */
data class RouteMemberLabels(
    val notStarted: String,
    val waiting: String,
    val setRoute: String
)

/**
 * 약속 상세로 이동 경로 화면(ROU-001) 상태를 만든다.
 * 제목, 멤버 요약, 약속 시각, 약속 장소, 멤버 목록은 약속 상세 값이다. 경로 선택지와 도착 예정 시각은
 * 출발 위치를 고른 뒤 이동 API로 받아 오므로 처음에는 비어 있다.
 * 서버 응답에는 내 멤버 ID가 없을 수 있어서, [myMemberId]를 알 수 없으면 방장일 때만 방장 멤버를 "나"로 본다.
 */
internal fun AppointmentDetail.toRouteUiState(
    memberSummary: String,
    labels: RouteMemberLabels,
    myMemberId: Long?
): RouteUiState {
    val routeMembers = members.map { member ->
        val isMe = if (myMemberId != null) {
            member.memberId == myMemberId
        } else {
            myRole == AppointmentMemberRole.HOST && member.role == AppointmentMemberRole.HOST
        }
        RouteMemberUiState(
            id = member.memberId.toString(),
            name = member.nickname,
            isMe = isMe,
            status = RouteMemberStatus.NOT_STARTED,
            statusLabel = labels.notStarted,
            description = if (isMe) labels.setRoute else labels.waiting,
            plannedTransport = if (isMe) null else member.travelMode?.toRouteTransport()
        )
    }.sortedByDescending { it.isMe }

    return RouteUiState(
        appointmentTitle = title,
        memberSummary = memberSummary,
        destinationName = placeSummary().orEmpty(),
        meetingTimeLabel = RouteTime.formatClockText(confirmedTime),
        members = routeMembers
    )
}

private fun AppointmentTravelMode.toRouteTransport(): RouteTransport? = when (this) {
    AppointmentTravelMode.BUS -> RouteTransport.BUS
    AppointmentTravelMode.SUBWAY -> RouteTransport.SUBWAY
    AppointmentTravelMode.TAXI -> RouteTransport.TAXI
    AppointmentTravelMode.OTHER -> null
}
