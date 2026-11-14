package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.AppointmentDetailDto
import com.jigmnnaga.data.remote.dto.AppointmentDetailPlaceDto
import com.jigmnnaga.data.remote.dto.AppointmentLocationDto
import com.jigmnnaga.data.remote.dto.AppointmentMemberDto
import com.jigmnnaga.domain.model.AppointmentAttendanceStatus
import com.jigmnnaga.domain.model.AppointmentDateSelectionType
import com.jigmnnaga.domain.model.AppointmentDetail
import com.jigmnnaga.domain.model.AppointmentDetailPlace
import com.jigmnnaga.domain.model.AppointmentLocation
import com.jigmnnaga.domain.model.AppointmentMember
import com.jigmnnaga.domain.model.AppointmentMemberRole
import com.jigmnnaga.domain.model.AppointmentTravelMode

internal fun AppointmentDetailDto.toDomain(): AppointmentDetail = AppointmentDetail(
    appointmentId = appointmentId,
    title = title,
    canceled = canceled,
    dateSelectionType = dateSelectionType.toEnum(AppointmentDateSelectionType.OTHER),
    candidateDates = candidateDates,
    candidateLocations = candidateLocations.map { it.toDomain() },
    responseDeadline = responseDeadline,
    inviteUrl = inviteUrl,
    confirmed = confirmed,
    confirmedDate = confirmedDate,
    confirmedTime = confirmedTime,
    confirmedLocation = confirmedLocation?.toDomain(),
    detailPlace = detailPlace?.toDomain(),
    myRole = myRole?.toEnum(AppointmentMemberRole.OTHER),
    myAttendanceStatus = myAttendanceStatus.toEnum(AppointmentAttendanceStatus.PENDING),
    myNotificationEnabled = myNotificationEnabled,
    myTravelMode = myTravelMode?.toEnum(AppointmentTravelMode.OTHER),
    joinedCount = joinedCount,
    scheduleSubmittedCount = scheduleSubmittedCount,
    members = members.map { it.toDomain() }
)

private fun AppointmentLocationDto.toDomain(): AppointmentLocation = AppointmentLocation(
    locationId = locationId,
    placeName = placeName.orEmpty(),
    address = address,
    lineNames = lineNames,
    latitude = latitude,
    longitude = longitude
)

private fun AppointmentDetailPlaceDto.toDomain(): AppointmentDetailPlace = AppointmentDetailPlace(
    placeName = placeName.orEmpty(),
    address = address,
    latitude = latitude,
    longitude = longitude
)

private fun AppointmentMemberDto.toDomain(): AppointmentMember = AppointmentMember(
    memberId = memberId,
    nickname = nickname,
    profileImageUrl = profileImageUrl,
    role = role.toEnum(AppointmentMemberRole.OTHER),
    scheduleSubmitted = scheduleSubmitted,
    attendanceStatus = attendanceStatus.toEnum(AppointmentAttendanceStatus.PENDING),
    travelMode = travelMode?.toEnum(AppointmentTravelMode.OTHER)
)

/** 대소문자를 가리지 않고 이름이 같은 값을 찾고, 값이 없거나 모르는 값이면 [fallback]을 돌려준다. */
private inline fun <reified E : Enum<E>> String?.toEnum(fallback: E): E {
    val name = this?.trim()?.uppercase() ?: return fallback
    return enumValues<E>().firstOrNull { it.name == name } ?: fallback
}
