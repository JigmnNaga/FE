package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.TravelCoordinateDto
import com.jigmnnaga.data.remote.dto.TravelOptionDto
import com.jigmnnaga.data.remote.dto.TravelOptionsDto
import com.jigmnnaga.data.remote.dto.TravelStepDto
import com.jigmnnaga.domain.model.TravelCoordinate
import com.jigmnnaga.domain.model.TravelMode
import com.jigmnnaga.domain.model.TravelOption
import com.jigmnnaga.domain.model.TravelOptions
import com.jigmnnaga.domain.model.TravelStep
import com.jigmnnaga.domain.model.TravelStepType

/** 이동수단 이름을 알 수 없으면 null. */
fun String?.toTravelMode(): TravelMode? = TravelMode.entries.firstOrNull { it.name == this }

fun TravelOptionsDto.toDomain(): TravelOptions = TravelOptions(
    destinationName = destinationName?.takeIf { it.isNotBlank() },
    appointmentDateTime = appointmentDateTime,
    departureTime = departureTime,
    departureRequired = departureRequired,
    myTravelMode = myTravelMode.toTravelMode(),
    // 모르는 이동수단은 화면이 그릴 수 없어서 뺀다.
    options = options.orEmpty().mapNotNull { it.toDomain() }
)

fun TravelOptionDto.toDomain(): TravelOption? {
    val travelMode = mode.toTravelMode() ?: return null
    return TravelOption(
        mode = travelMode,
        durationMinutes = durationMinutes,
        distanceMeters = distanceMeters,
        estimatedArrivalTime = estimatedArrivalTime,
        steps = steps.orEmpty().map { it.toDomain() },
        path = path.orEmpty().map { it.toDomain() }
    )
}

fun TravelStepDto.toDomain(): TravelStep = TravelStep(
    type = when (type) {
        "WALKING", "WALK" -> TravelStepType.WALK
        "BUS" -> TravelStepType.BUS
        "SUBWAY" -> TravelStepType.SUBWAY
        "TAXI" -> TravelStepType.TAXI
        else -> TravelStepType.OTHER
    },
    minutes = minutes,
    name = name?.takeIf { it.isNotBlank() }
)

fun TravelCoordinateDto.toDomain(): TravelCoordinate = TravelCoordinate(latitude, longitude)
