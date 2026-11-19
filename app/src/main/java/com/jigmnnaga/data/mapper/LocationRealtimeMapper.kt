package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.ArrivalBroadcastDto
import com.jigmnnaga.data.remote.dto.EtaBroadcastDto
import com.jigmnnaga.data.remote.dto.LocationBroadcastDto
import com.jigmnnaga.data.remote.dto.LocationSharingEndedDto
import com.jigmnnaga.domain.model.LocationShareEnd
import com.jigmnnaga.domain.model.LocationShareEndReason
import com.jigmnnaga.domain.model.LocationShareError
import com.jigmnnaga.domain.model.MemberArrival
import com.jigmnnaga.domain.model.MemberDepartureStatus
import com.jigmnnaga.domain.model.MemberEtaUpdate
import com.jigmnnaga.domain.model.MemberLocationUpdate

internal fun LocationBroadcastDto.toDomain() = MemberLocationUpdate(
    memberId = memberId,
    latitude = latitude,
    longitude = longitude,
    departureStatus = when (departureStatus) {
        "BEFORE_DEPARTURE" -> MemberDepartureStatus.BEFORE_DEPARTURE
        "DEPARTED" -> MemberDepartureStatus.DEPARTED
        "ARRIVED" -> MemberDepartureStatus.ARRIVED
        else -> MemberDepartureStatus.UNKNOWN
    },
    updatedAt = updatedAt
)

internal fun EtaBroadcastDto.toDomain() = MemberEtaUpdate(
    memberId = memberId,
    estimatedArrivalTime = estimatedArrivalTime,
    remainingMinutes = remainingMinutes,
    calculatedAt = calculatedAt,
    expectedLatenessMinutes = expectedLatenessMinutes,
    isExpectedLate = isExpectedLate
)

internal fun ArrivalBroadcastDto.toDomain() = MemberArrival(memberId = memberId, arrivedAt = arrivedAt)

internal fun LocationSharingEndedDto.toDomain() = LocationShareEnd(
    memberId = memberId,
    reason = when (reason) {
        "ARRIVED" -> LocationShareEndReason.ARRIVED
        "USER_REQUEST" -> LocationShareEndReason.USER_REQUEST
        else -> LocationShareEndReason.UNKNOWN
    },
    endedAt = endedAt
)

/** `/user/queue/errors`의 오류 코드를 위치 공유 오류로 바꾼다. 위치와 상관없는 코드(채팅 등)는 null. */
internal fun locationShareErrorOf(code: String?): LocationShareError? = when (code) {
    "NOT_DEPARTED" -> LocationShareError.NOT_DEPARTED
    "LOCATION_SHARING_STOPPED" -> LocationShareError.SHARING_STOPPED
    "LOCATION_UPDATE_TOO_FREQUENT" -> LocationShareError.TOO_FREQUENT
    "NOT_APPOINTMENT_PARTICIPANT", "APPOINTMENT_CANCELED", "TRAVEL_DESTINATION_NOT_CONFIRMED" -> LocationShareError.OTHER
    else -> null
}
