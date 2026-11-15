package com.jigmnnaga.presentation.appointment.route

import com.jigmnnaga.domain.model.AppointmentDetail
import com.jigmnnaga.domain.model.AppointmentMemberRole
import com.jigmnnaga.presentation.appointment.detail.placeSummary

/** 이동 경로 화면 멤버 카드에 보여 줄 문구. */
data class RouteMemberLabels(
    val notStarted: String,
    val waiting: String,
    val setRoute: String
)

/**
 * 약속 상세로 이동 경로 화면(ROU-001) 상태를 만든다.
 * 제목, 멤버 요약, 약속 장소, 멤버 목록은 약속 상세 값이다. 경로 선택지와 도착 예정 시각은
 * 이동 경로 API(이동·실시간 위치 업무)가 붙기 전까지 [RouteSampleData]의 예시 값을 그대로 쓴다.
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
            description = if (isMe) labels.setRoute else labels.waiting
        )
    }.sortedByDescending { it.isMe }

    return RouteSampleData.overview.copy(
        appointmentTitle = title,
        memberSummary = memberSummary,
        destinationName = placeSummary().orEmpty(),
        members = routeMembers
    )
}
