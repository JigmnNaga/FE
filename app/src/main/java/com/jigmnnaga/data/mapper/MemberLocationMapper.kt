package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.MemberLocationDto
import com.jigmnnaga.domain.model.MemberLocation

fun MemberLocationDto.toDomain(): MemberLocation = MemberLocation(
    locationId = locationId,
    name = name,
    address = address?.takeIf { it.isNotBlank() },
    latitude = latitude,
    longitude = longitude
)
