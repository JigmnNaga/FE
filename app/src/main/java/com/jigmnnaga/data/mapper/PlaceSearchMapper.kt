package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.PlaceDto
import com.jigmnnaga.data.remote.dto.PlaceSearchResponseDto
import com.jigmnnaga.domain.model.PlaceSearchPage
import com.jigmnnaga.domain.model.SearchedPlace

fun PlaceSearchResponseDto.toDomain(): PlaceSearchPage = PlaceSearchPage(
    places = places.map { it.toDomain() },
    isEnd = isEnd
)

fun PlaceDto.toDomain(): SearchedPlace = SearchedPlace(
    kakaoPlaceId = kakaoPlaceId,
    name = placeName,
    lineNames = lineNames?.takeIf { it.isNotBlank() },
    roadAddress = roadAddress?.takeIf { it.isNotBlank() },
    jibunAddress = jibunAddress?.takeIf { it.isNotBlank() },
    latitude = latitude,
    longitude = longitude
)
