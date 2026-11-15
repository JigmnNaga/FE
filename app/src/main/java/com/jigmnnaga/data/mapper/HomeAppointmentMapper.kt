package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.MyAppointmentResponseDto
import com.jigmnnaga.domain.model.HomeAppointment

internal fun MyAppointmentResponseDto.toDomain(): HomeAppointment = HomeAppointment(
    appointmentId = appointmentId,
    title = title,
    canceled = canceled,
    confirmed = confirmed,
    confirmedDate = confirmedDate,
    confirmedTime = confirmedTime,
    confirmedLocationName = confirmedLocationName,
    detailPlaceName = detailPlaceName,
    candidateStartDate = candidateStartDate,
    candidateEndDate = candidateEndDate,
    memberCount = memberCount,
    unreadMessageCount = unreadMessageCount
)
