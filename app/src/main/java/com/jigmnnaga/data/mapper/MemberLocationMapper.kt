package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.MemberLocationDto
import com.jigmnnaga.data.remote.dto.MemberLocationRequestDto
import com.jigmnnaga.domain.model.MemberLocation
import com.jigmnnaga.domain.model.MemberLocationInput

fun MemberLocationDto.toDomain(): MemberLocation = MemberLocation(
    locationId = locationId,
    name = name,
    address = address?.takeIf { it.isNotBlank() },
    latitude = latitude,
    longitude = longitude
)

/** 이름 앞뒤 공백은 서버도 지우지만, 빈 이름은 보내기 전에 걸러야 해서 여기서 먼저 정리한다. */
fun MemberLocationInput.toRequestDto(): MemberLocationRequestDto = MemberLocationRequestDto(
    name = name.trim(),
    address = address?.trim()?.takeIf { it.isNotEmpty() },
    latitude = latitude,
    longitude = longitude
)
