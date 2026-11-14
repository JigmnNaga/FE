package com.jigmnnaga.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageDto(
    val messageId: Long,
    val appointmentId: Long? = null,
    val type: String? = null,
    val senderId: Long? = null,
    val senderNickname: String? = null,
    val senderProfileImageUrl: String? = null,
    val content: String = "",
    val sentAt: String? = null,
    val unreadCount: Int? = null
)

@Serializable
data class ChatPageDto(
    val messages: List<ChatMessageDto> = emptyList(),
    val myLastReadMessageId: Long? = null,
    val nextCursor: Long? = null,
    val hasNext: Boolean = false
)

@Serializable
data class ChatReadRequestDto(val lastReadMessageId: Long)

/** 읽음 처리 REST 응답과 `/sub/appointments/{id}/read` 알림이 같은 모양이다. */
@Serializable
data class ChatReadDto(
    val memberId: Long,
    val previousLastReadMessageId: Long? = null,
    val lastReadMessageId: Long
)

@Serializable
data class ChatSendRequestDto(val content: String)
