package com.jigmnnaga.domain.model

/** 확정된 약속에 보낼 수 있는 참석 응답. 응답 전(PENDING)은 보낼 수 없다. */
enum class AttendanceAnswer {
    /** 참석할게요 */
    ATTENDING,

    /** 참석이 어려워요 */
    NOT_ATTENDING,

    /** 일정 조율 요청 */
    RESCHEDULE_REQUESTED
}

/** 서버가 참석 응답을 받아 주지 않은 이유. */
enum class AttendanceRejectReason {
    APPOINTMENT_CANCELED,

    /** 아직 일정·장소가 확정되지 않은 약속. */
    NOT_CONFIRMED,

    /** 약속 시간이 이미 지남. */
    TIME_PASSED,
    INVALID_ANSWER
}

/** 참석 응답 API 호출 결과. */
sealed interface AttendanceResult<out T> {
    data class Success<T>(val value: T) : AttendanceResult<T>
    data object LoginRequired : AttendanceResult<Nothing>

    /** 약속에 참여한 멤버가 아니거나 약속이 없을 때. */
    data object NotParticipant : AttendanceResult<Nothing>
    data class Rejected(val reason: AttendanceRejectReason) : AttendanceResult<Nothing>
    data class Failure(val retryable: Boolean) : AttendanceResult<Nothing>
}
