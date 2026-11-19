package com.jigmnnaga.domain.model

/** 위치 공유 중인 멤버의 출발 상태. 모르는 값은 [UNKNOWN]으로 받는다. */
enum class MemberDepartureStatus { BEFORE_DEPARTURE, DEPARTED, ARRIVED, UNKNOWN }

/** 멤버 한 명의 최신 위치. 시각은 서버 문자열(`yyyy-MM-dd'T'HH:mm:ss`) 그대로 둔다. */
data class MemberLocationUpdate(
    val memberId: Long,
    val latitude: Double,
    val longitude: Double,
    val departureStatus: MemberDepartureStatus,
    val updatedAt: String? = null
)

/** 멤버 한 명의 도착 예정 정보. 서버가 30초 간격으로, 도착하기 전에만 보낸다. */
data class MemberEtaUpdate(
    val memberId: Long,
    val estimatedArrivalTime: String? = null,
    val remainingMinutes: Int? = null,
    val calculatedAt: String? = null,
    val expectedLatenessMinutes: Int? = null,
    val isExpectedLate: Boolean? = null
)

/** 멤버가 약속 장소에 도착했다. */
data class MemberArrival(
    val memberId: Long,
    val arrivedAt: String? = null
)

enum class LocationShareEndReason { ARRIVED, USER_REQUEST, UNKNOWN }

/** 멤버의 위치 공유가 끝났다. */
data class LocationShareEnd(
    val memberId: Long,
    val reason: LocationShareEndReason,
    val endedAt: String? = null
)

/** 내 위치를 보냈을 때 서버가 거절한 이유. */
enum class LocationShareError {
    /** 출발하기 전이다. */
    NOT_DEPARTED,

    /** 이 약속의 위치 공유가 꺼져 있다. */
    SHARING_STOPPED,

    /** 1초 안에 다시 보냈다. 다음 갱신에서 자연히 해소된다. */
    TOO_FREQUENT,

    /** 그 밖의 이유(참여자가 아니거나 약속이 취소됨, 좌표 오류 등). */
    OTHER
}
