package com.jigmnnaga.domain.model

/**
 * 홈 화면 약속 목록 아이템.
 *
 * 날짜는 서버 문자열 그대로 보관 (minSdk 24, java.time 미사용).
 * - [confirmedDate]: `yyyy-MM-dd` (미확정이면 null)
 * - [confirmedTime]: `HH:mm` (미확정이면 null)
 * - [candidateStartDate], [candidateEndDate]: `yyyy-MM-dd`
 */
data class HomeAppointment(
    val appointmentId: Long,
    val title: String,
    val canceled: Boolean,
    val confirmed: Boolean,
    val confirmedDate: String?,
    val confirmedTime: String?,
    val confirmedLocationName: String?,
    val detailPlaceName: String?,
    val candidateStartDate: String?,
    val candidateEndDate: String?,
    val memberCount: Int,
    val unreadMessageCount: Int
)

/** 홈 화면 약속 목록 조회 결과. */
sealed interface HomeAppointmentListResult {
    data class Success(val appointments: List<HomeAppointment>) : HomeAppointmentListResult
    data object LoginRequired : HomeAppointmentListResult
    data class Failure(val retryable: Boolean) : HomeAppointmentListResult
}

/** 홈 화면 다음 약속 조회 결과. [appointment]가 null이면 다음 확정 약속 없음. */
sealed interface NextAppointmentResult {
    data class Success(val appointment: HomeAppointment?) : NextAppointmentResult
    data object LoginRequired : NextAppointmentResult
    data class Failure(val retryable: Boolean) : NextAppointmentResult
}

/** 대기 중인 초대 수 조회 결과. */
sealed interface PendingInviteCountResult {
    data class Success(val count: Int) : PendingInviteCountResult
    data object LoginRequired : PendingInviteCountResult
    data class Failure(val retryable: Boolean) : PendingInviteCountResult
}
