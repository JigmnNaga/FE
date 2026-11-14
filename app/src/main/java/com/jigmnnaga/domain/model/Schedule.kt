package com.jigmnnaga.domain.model

/**
 * 하루치 가능 시간. minSdk 24에서는 java.time을 쓸 수 없어서 서버가 보낸 문자열 그대로 둔다.
 * [date]는 `yyyy-MM-dd`, [startTime]·[endTime]은 `HH:mm` 형식이다.
 */
data class ScheduleAvailability(
    val date: String,
    val startTime: String,
    val endTime: String
)

/** 내가 제출한 일정. 아직 제출하지 않았으면 [submitted]가 false이고 목록은 비어 있다. */
data class MySchedule(
    val submitted: Boolean,
    val availabilities: List<ScheduleAvailability>,
    val preferredLocationId: Long?
)

/** 제출할 일정. 서버는 기존 선택을 전부 새 값으로 바꾸고, 희망 장소는 약속의 후보 장소 중 하나여야 한다. */
data class ScheduleSubmission(
    val availabilities: List<ScheduleAvailability>,
    val preferredLocationId: Long
)

data class MemberSchedule(
    val memberId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val submitted: Boolean,
    val availabilities: List<ScheduleAvailability>,
    val preferredLocationId: Long?
)

data class ScheduleVoter(
    val memberId: Long,
    val nickname: String,
    val profileImageUrl: String?
)

/** 후보 장소 하나에 대한 선택 현황. */
data class LocationVote(
    val locationId: Long,
    val placeName: String,
    val address: String?,
    val lineNames: String?,
    val voteCount: Int,
    val topVoted: Boolean,
    val voters: List<ScheduleVoter>
)

/** 참여자 전체의 일정 응답 현황과 장소별 선택 현황. */
data class ScheduleStatus(
    val totalCount: Int,
    val submittedCount: Int,
    val members: List<MemberSchedule>,
    val locationVotes: List<LocationVote>
)

/** 서버가 일정 제출을 받아 주지 않은 이유. */
enum class ScheduleRejectReason {
    APPOINTMENT_CANCELED,
    DEADLINE_PASSED,
    INVALID_DATE,
    DUPLICATE_DATE,
    INVALID_TIME,
    INVALID_LOCATION
}

/** 일정 API 호출 결과. */
sealed interface ScheduleResult<out T> {
    data class Success<T>(val value: T) : ScheduleResult<T>
    data object LoginRequired : ScheduleResult<Nothing>

    /** 약속에 참여한 멤버가 아니거나 약속이 없을 때. */
    data object NotParticipant : ScheduleResult<Nothing>
    data class Rejected(val reason: ScheduleRejectReason) : ScheduleResult<Nothing>
    data class Failure(val retryable: Boolean) : ScheduleResult<Nothing>
}
