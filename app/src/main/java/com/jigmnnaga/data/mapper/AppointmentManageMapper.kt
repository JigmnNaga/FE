package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.CandidateLocationsUpdateRequestDto
import com.jigmnnaga.data.remote.dto.ReminderSendResponseDto
import com.jigmnnaga.domain.model.CandidateLocationChange
import com.jigmnnaga.domain.model.ReminderCount
import com.jigmnnaga.domain.model.ReminderSendSummary

fun CandidateLocationChange.toDto() = CandidateLocationsUpdateRequestDto(
    keepLocationIds = keepLocationIds,
    newLocations = newLocations.map { it.toDto() }
)

fun ReminderSendResponseDto.toDomain() = ReminderSendSummary(
    sentCount = sentCount,
    reminders = reminders.map { ReminderCount(type = it.type, sentCount = it.sentCount) }
)
