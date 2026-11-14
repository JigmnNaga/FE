package com.jigmnnaga.domain.model

/**
 * 방장이 최종 확정할 때 고를 수 있는 시간 후보. 가능 인원이 많은 연속 시간대다.
 * minSdk 24에서는 java.time을 쓸 수 없어서 서버가 보낸 문자열 그대로 둔다.
 * [date]는 `yyyy-MM-dd`, [startTime]·[endTime]은 `HH:mm` 형식이다.
 */
data class TimeCandidate(
    val date: String,
    val startTime: String,
    val endTime: String,
    val availableCount: Int
)

/** 날짜·시간 후보 조회 결과. 시간대 후보는 가능 인원이 많은 순서다. */
data class TimeCandidates(
    val totalMemberCount: Int,
    val candidates: List<TimeCandidate>
)

/** 확정할 세부 위치(상호). 세부 위치를 나중에 정하면 보내지 않는다. */
data class ConfirmDetailPlace(
    val placeName: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double
)

/** 서버가 확정을 받아 주지 않은 이유. */
enum class ConfirmationRejectReason {
    APPOINTMENT_CANCELED,

    /** 후보 날짜에 없는 날짜. */
    INVALID_DATE,

    /** 후보 장소가 아닌 장소. */
    INVALID_LOCATION
}

/** 확정 관련 API 호출 결과. */
sealed interface ConfirmationResult<out T> {
    data class Success<T>(val value: T) : ConfirmationResult<T>
    data object LoginRequired : ConfirmationResult<Nothing>

    /** 방장이 아니거나 약속이 없을 때. */
    data object NotHost : ConfirmationResult<Nothing>
    data class Rejected(val reason: ConfirmationRejectReason) : ConfirmationResult<Nothing>
    data class Failure(val retryable: Boolean) : ConfirmationResult<Nothing>
}
