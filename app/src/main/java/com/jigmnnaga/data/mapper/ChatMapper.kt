package com.jigmnnaga.data.mapper

import com.jigmnnaga.data.remote.dto.ChatMessageDto
import com.jigmnnaga.data.remote.dto.ChatPageDto
import com.jigmnnaga.data.remote.dto.ChatReadDto
import com.jigmnnaga.domain.model.ChatMessage
import com.jigmnnaga.domain.model.ChatMessageType
import com.jigmnnaga.domain.model.ChatPage
import com.jigmnnaga.domain.model.ChatReadEvent

fun ChatMessageDto.toDomain() = ChatMessage(
    id = messageId,
    type = if (type == "SYSTEM") ChatMessageType.SYSTEM else ChatMessageType.TEXT,
    senderId = senderId,
    senderNickname = senderNickname,
    content = content,
    sentAt = sentAt,
    unreadCount = unreadCount
)

fun ChatPageDto.toDomain() = ChatPage(
    messages = messages.map { it.toDomain() }.sortedBy { it.id },
    myLastReadMessageId = myLastReadMessageId,
    nextCursor = nextCursor,
    hasNext = hasNext
)

fun ChatReadDto.toDomain() = ChatReadEvent(
    memberId = memberId,
    previousLastReadMessageId = previousLastReadMessageId,
    lastReadMessageId = lastReadMessageId
)
