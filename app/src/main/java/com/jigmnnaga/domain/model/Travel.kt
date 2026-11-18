package com.jigmnnaga.domain.model

/** 약속 장소까지 갈 이동수단. */
enum class TravelMode { BUS, SUBWAY, TAXI }

/** 이동 경로 한 구간의 종류. 모르는 값은 [OTHER]로 받는다. */
enum class TravelStepType { WALK, BUS, SUBWAY, TAXI, OTHER }

/** 이동 경로의 한 구간. [name]은 노선·차량 이름이고 없을 수 있다. */
data class TravelStep(
    val type: TravelStepType,
    val minutes: Int,
    val name: String? = null
)

data class TravelCoordinate(
    val latitude: Double,
    val longitude: Double
)

/**
 * 이동수단 하나의 대표 경로.
 * minSdk 24에서는 java.time을 쓸 수 없어서 시각은 서버가 보낸 `yyyy-MM-dd'T'HH:mm:ss` 문자열 그대로 둔다.
 */
data class TravelOption(
    val mode: TravelMode,
    val durationMinutes: Int,
    val distanceMeters: Int,
    val estimatedArrivalTime: String? = null,
    val steps: List<TravelStep> = emptyList(),
    val path: List<TravelCoordinate> = emptyList()
)

/** 이동수단 후보 조회 결과. 일부 이동수단은 경로를 찾지 못하면 [options]에서 빠진다. */
data class TravelOptions(
    val destinationName: String? = null,
    val appointmentDateTime: String? = null,
    val departureTime: String? = null,
    val departureRequired: Boolean? = null,
    val myTravelMode: TravelMode? = null,
    val options: List<TravelOption> = emptyList()
)

/** 서버가 요청을 받아들이지 않은 이유. */
enum class TravelRejectReason {
    DESTINATION_NOT_CONFIRMED,
    TIME_PASSED,
    DEPARTURE_LOCATION_NOT_REGISTERED,
    ALREADY_DEPARTED,
    ALREADY_ARRIVED,
    NOT_YET_DEPARTED,
    INVALID_INPUT,
    NOT_PARTICIPANT
}

/** 이동 API 호출 결과. */
sealed interface TravelResult<out T> {
    data class Success<T>(val value: T) : TravelResult<T>
    data object LoginRequired : TravelResult<Nothing>
    data class Rejected(val reason: TravelRejectReason) : TravelResult<Nothing>
    data class Failure(val retryable: Boolean) : TravelResult<Nothing>
}
