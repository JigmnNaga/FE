package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.AppointmentCreateRequestDto
import com.jigmnnaga.data.remote.dto.AppointmentCreateResponseDto
import com.jigmnnaga.data.remote.dto.CreateCandidateLocationDto
import com.jigmnnaga.data.remote.dto.CreatePreviousMembersDto
import com.jigmnnaga.domain.model.CreatedAppointment
import com.jigmnnaga.domain.model.NewAppointment
import com.jigmnnaga.domain.model.NewCandidateLocation
import com.jigmnnaga.domain.model.PreviousMembers

fun NewAppointment.toDto(): AppointmentCreateRequestDto = AppointmentCreateRequestDto(
    title = title,
    dateSelectionType = dateType.name,
    candidateDates = candidateDates,
    candidateLocations = candidateLocations.map { it.toDto() },
    responseDeadline = responseDeadline,
    previousMembers = previousMembers?.toDto()
)

private fun NewCandidateLocation.toDto() = CreateCandidateLocationDto(
    placeName = placeName,
    address = address,
    lineNames = lineNames,
    latitude = latitude,
    longitude = longitude
)

private fun PreviousMembers.toDto() = CreatePreviousMembersDto(
    appointmentId = appointmentId,
    memberIds = memberIds
)

fun AppointmentCreateResponseDto.toDomain() = CreatedAppointment(
    appointmentId = appointmentId,
    inviteCode = inviteCode,
    inviteUrl = inviteUrl
)
