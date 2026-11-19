package com.jigmnnaga.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LocationSendRequestDto(val latitude: Double, val longitude: Double)

/** `/sub/appointments/{id}/location` */
@Serializable
data class LocationBroadcastDto(
    val memberId: Long,
    val latitude: Double,
    val longitude: Double,
    val departureStatus: String? = null,
    val updatedAt: String? = null
)

/** `/sub/appointments/{id}/eta` */
@Serializable
data class EtaBroadcastDto(
    val memberId: Long,
    val estimatedArrivalTime: String? = null,
    val remainingMinutes: Int? = null,
    val calculatedAt: String? = null,
    val expectedLatenessMinutes: Int? = null,
    val isExpectedLate: Boolean? = null
)

/** `/sub/appointments/{id}/arrival` */
@Serializable
data class ArrivalBroadcastDto(
    val memberId: Long,
    val arrivedAt: String? = null
)

/** `/sub/appointments/{id}/location-end` */
@Serializable
data class LocationSharingEndedDto(
    val memberId: Long,
    val reason: String? = null,
    val endedAt: String? = null
)
