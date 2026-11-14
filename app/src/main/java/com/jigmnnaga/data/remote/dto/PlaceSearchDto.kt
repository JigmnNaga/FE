package com.jigmnnaga.data.remote.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/** 장소 검색(`GET api/v1/places/search`) 응답. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PlaceSearchResponseDto(
    val places: List<PlaceDto> = emptyList(),
    @JsonNames("end") val isEnd: Boolean = true
)

@Serializable
data class PlaceDto(
    val kakaoPlaceId: String,
    val placeName: String,
    val lineNames: String? = null,
    val roadAddress: String? = null,
    val jibunAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
