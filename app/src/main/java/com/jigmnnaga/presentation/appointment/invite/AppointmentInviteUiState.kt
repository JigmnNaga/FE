package com.jigmnnaga.presentation.appointment.invite

/** 서버/API와 분리된 APT-002 화면 표시 데이터입니다. */
data class AppointmentInviteUiState(
    val appointmentName: String,
    val inviterName: String,
    val members: List<AppointmentInviteMember>,
    val result: AppointmentInviteResult = AppointmentInviteResult.PENDING
) {
    val participatingMemberCount: Int
        get() = members.count {
            it.status == AppointmentInviteMemberStatus.HOST ||
                it.status == AppointmentInviteMemberStatus.ACCEPTED
        }
}

data class AppointmentInviteMember(
    val name: String,
    val status: AppointmentInviteMemberStatus
)

enum class AppointmentInviteMemberStatus {
    HOST,
    ACCEPTED,
    DECLINED,
    WAITING
}

/** 결과 화면은 실제 응답을 받은 상위 계층이 전달합니다. */
enum class AppointmentInviteResult {
    PENDING,
    ACCEPTED,
    DECLINED
}
