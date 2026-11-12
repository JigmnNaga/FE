package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.InviteDetailDto
import com.jigmnnaga.data.remote.dto.InviteParticipantDto
import com.jigmnnaga.domain.model.AppointmentInvite
import com.jigmnnaga.domain.model.InviteParticipant
import com.jigmnnaga.domain.model.InviteParticipantRole
import com.jigmnnaga.domain.model.InviteParticipationStatus

internal fun InviteDetailDto.toDomain(): AppointmentInvite = AppointmentInvite(
    appointmentId = appointmentId,
    title = title,
    canceled = canceled,
    hostNickname = hostNickname,
    responseDeadline = responseDeadline,
    expired = expired,
    respondedCount = respondedCount,
    joinedCount = joinedCount,
    myStatus = myStatus?.toParticipationStatus() ?: InviteParticipationStatus.PENDING,
    myRole = myRole?.toParticipantRole(),
    participants = participants.map { it.toDomain() }
)

internal fun InviteParticipantDto.toDomain(): InviteParticipant = InviteParticipant(
    memberId = memberId,
    nickname = nickname,
    profileImageUrl = profileImageUrl,
    role = role?.toParticipantRole() ?: InviteParticipantRole.OTHER,
    status = status?.toParticipationStatus() ?: InviteParticipationStatus.OTHER
)

private fun String.toParticipantRole(): InviteParticipantRole = when (uppercase()) {
    "HOST" -> InviteParticipantRole.HOST
    "MEMBER" -> InviteParticipantRole.MEMBER
    else -> InviteParticipantRole.OTHER
}

private fun String.toParticipationStatus(): InviteParticipationStatus = when (uppercase()) {
    "PENDING" -> InviteParticipationStatus.PENDING
    "ACCEPTED" -> InviteParticipationStatus.ACCEPTED
    "DECLINED" -> InviteParticipationStatus.DECLINED
    "KICKED" -> InviteParticipationStatus.KICKED
    else -> InviteParticipationStatus.OTHER
}
