package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.AvailabilityDto
import com.jigmnnaga.data.remote.dto.LocationVoteDto
import com.jigmnnaga.data.remote.dto.MemberScheduleDto
import com.jigmnnaga.data.remote.dto.MyScheduleDto
import com.jigmnnaga.data.remote.dto.ScheduleStatusDto
import com.jigmnnaga.data.remote.dto.ScheduleSubmitRequestDto
import com.jigmnnaga.data.remote.dto.ScheduleVoterDto
import com.jigmnnaga.domain.model.LocationVote
import com.jigmnnaga.domain.model.MemberSchedule
import com.jigmnnaga.domain.model.MySchedule
import com.jigmnnaga.domain.model.ScheduleAvailability
import com.jigmnnaga.domain.model.ScheduleStatus
import com.jigmnnaga.domain.model.ScheduleSubmission
import com.jigmnnaga.domain.model.ScheduleVoter

internal fun ScheduleSubmission.toDto(): ScheduleSubmitRequestDto = ScheduleSubmitRequestDto(
    availabilities = availabilities.map { AvailabilityDto(it.date, it.startTime, it.endTime) },
    preferredLocationId = preferredLocationId
)

internal fun MyScheduleDto.toDomain(): MySchedule = MySchedule(
    submitted = submitted,
    availabilities = availabilities.map { it.toDomain() },
    preferredLocationId = preferredLocationId
)

internal fun ScheduleStatusDto.toDomain(): ScheduleStatus = ScheduleStatus(
    totalCount = totalCount,
    submittedCount = submittedCount,
    members = members.map { it.toDomain() },
    locationVotes = locationVotes.map { it.toDomain() }
)

private fun AvailabilityDto.toDomain() = ScheduleAvailability(date, startTime, endTime)

private fun MemberScheduleDto.toDomain() = MemberSchedule(
    memberId = memberId,
    nickname = nickname,
    profileImageUrl = profileImageUrl,
    submitted = submitted,
    availabilities = availabilities.map { it.toDomain() },
    preferredLocationId = preferredLocationId
)

private fun LocationVoteDto.toDomain() = LocationVote(
    locationId = locationId,
    placeName = placeName.orEmpty(),
    address = address,
    lineNames = lineNames,
    voteCount = voteCount,
    topVoted = topVoted,
    voters = voters.map { it.toDomain() }
)

private fun ScheduleVoterDto.toDomain() = ScheduleVoter(memberId, nickname, profileImageUrl)
