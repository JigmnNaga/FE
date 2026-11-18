package com.jigmnnaga.data.remote.dto

import kotlinx.serialization.Serializable

/** 출발 위치 등록(`PUT api/v1/appointments/{id}/travel/departure-location`) 요청. */
@Serializable
data class DepartureLocationRequestDto(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

/** 이동수단 선택(`PUT api/v1/appointments/{id}/travel/mode`) 요청. */
@Serializable
data class TravelModeRequestDto(
    val mode: String,
    val durationMinutes: Int
)

/** 이동수단 후보 조회(`GET api/v1/appointments/{id}/travel/options`) 응답. */
@Serializable
data class TravelOptionsDto(
    val destinationName: String? = null,
    val appointmentDateTime: String? = null,
    val departureTime: String? = null,
    val departureRequired: Boolean? = null,
    val myTravelMode: String? = null,
    val options: List<TravelOptionDto>? = null
)

@Serializable
data class TravelOptionDto(
    val mode: String? = null,
    val durationMinutes: Int = 0,
    val distanceMeters: Int = 0,
    val estimatedArrivalTime: String? = null,
    val steps: List<TravelStepDto>? = null,
    val path: List<TravelCoordinateDto>? = null
)

@Serializable
data class TravelStepDto(
    val type: String? = null,
    val minutes: Int = 0,
    val name: String? = null
)

@Serializable
data class TravelCoordinateDto(
    val latitude: Double,
    val longitude: Double
)
