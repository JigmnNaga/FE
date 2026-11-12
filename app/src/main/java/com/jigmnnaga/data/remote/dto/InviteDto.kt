package com.jigmnnaga.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InviteDetailDto(
    val appointmentId: Long,
    val title: String,
    val canceled: Boolean = false,
    val hostNickname: String? = null,
    val responseDeadline: String? = null,
    val expired: Boolean = false,
    val respondedCount: Int = 0,
    val joinedCount: Int = 0,
    val myStatus: String? = null,
    val myRole: String? = null,
    val participants: List<InviteParticipantDto> = emptyList()
)

@Serializable
data class InviteParticipantDto(
    val memberId: Long,
    val nickname: String,
    val profileImageUrl: String? = null,
    val role: String? = null,
    val status: String? = null
)

@Serializable
data class InviteAcceptDto(
    val appointmentId: Long
)
