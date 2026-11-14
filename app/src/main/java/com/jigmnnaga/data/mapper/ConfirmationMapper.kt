package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.DetailPlaceDto
import com.jigmnnaga.data.remote.dto.ScheduleHeatmapDto
import com.jigmnnaga.domain.model.ConfirmDetailPlace
import com.jigmnnaga.domain.model.TimeCandidate
import com.jigmnnaga.domain.model.TimeCandidates

internal fun ConfirmDetailPlace.toDto(): DetailPlaceDto = DetailPlaceDto(
    placeName = placeName,
    address = address,
    latitude = latitude,
    longitude = longitude
)

internal fun ScheduleHeatmapDto.toDomain(): TimeCandidates = TimeCandidates(
    totalMemberCount = totalMemberCount,
    candidates = topCandidates.map {
        TimeCandidate(
            date = it.date,
            startTime = it.startTime,
            endTime = it.endTime,
            availableCount = it.availableCount
        )
    }
)
