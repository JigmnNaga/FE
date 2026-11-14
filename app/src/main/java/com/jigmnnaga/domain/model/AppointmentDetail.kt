package com.jigmnnaga.domain.model

/** 서버가 보내는 날짜 선택 방식. 모르는 값은 [OTHER]로 받는다. */
enum class AppointmentDateSelectionType { SINGLE, RANGE, MULTIPLE, OTHER }

enum class AppointmentMemberRole { HOST, MEMBER, OTHER }

enum class AppointmentAttendanceStatus {
    PENDING,
    ATTENDING,
    NOT_ATTENDING,
    RESCHEDULE_REQUESTED,
    OTHER
}

enum class AppointmentTravelMode { BUS, SUBWAY, TAXI, OTHER }

/** 후보 또는 확정된 장소. 좌표는 서버가 주지 않으면 null. */
data class AppointmentLocation(
    val locationId: Long?,
    val placeName: String,
    val address: String?,
    val lineNames: String?,
    val latitude: Double?,
    val longitude: Double?
)

/** 확정된 세부 장소(예: 카페 이름). */
data class AppointmentDetailPlace(
    val placeName: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?
)

data class AppointmentMember(
    val memberId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val role: AppointmentMemberRole,
    val scheduleSubmitted: Boolean,
    val attendanceStatus: AppointmentAttendanceStatus,
    val travelMode: AppointmentTravelMode?
)

/**
 * 약속 상세.
 * minSdk 24에서는 java.time을 쓸 수 없어서 날짜·시간은 서버가 보낸 문자열 그대로 둔다.
 * [candidateDates]와 [confirmedDate]는 `yyyy-MM-dd`, [confirmedTime]은 `HH:mm`,
 * [responseDeadline]은 `yyyy-MM-dd'T'HH:mm:ss` 형식이다.
 */
data class AppointmentDetail(
    val appointmentId: Long,
    val title: String,
    val canceled: Boolean,
    val dateSelectionType: AppointmentDateSelectionType,
    val candidateDates: List<String>,
    val candidateLocations: List<AppointmentLocation>,
    val responseDeadline: String?,
    val inviteUrl: String?,
    val confirmed: Boolean,
    val confirmedDate: String?,
    val confirmedTime: String?,
    val confirmedLocation: AppointmentLocation?,
    val detailPlace: AppointmentDetailPlace?,
    val myRole: AppointmentMemberRole?,
    val myAttendanceStatus: AppointmentAttendanceStatus,
    val myNotificationEnabled: Boolean,
    val myTravelMode: AppointmentTravelMode?,
    val joinedCount: Int,
    val scheduleSubmittedCount: Int,
    val members: List<AppointmentMember>
)

/** 약속 상세 조회 결과. */
sealed interface AppointmentDetailResult<out T> {
    data class Success<T>(val value: T) : AppointmentDetailResult<T>
    data object LoginRequired : AppointmentDetailResult<Nothing>

    /** 약속에 참여한 멤버가 아니거나 약속이 없을 때. */
    data object NotParticipant : AppointmentDetailResult<Nothing>
    data class Failure(val retryable: Boolean) : AppointmentDetailResult<Nothing>
}
