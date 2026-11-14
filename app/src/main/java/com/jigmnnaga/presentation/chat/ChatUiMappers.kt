package com.jigmnnaga.presentation.chat

import androidx.annotation.StringRes
import com.jigmnnaga.R
import com.jigmnnaga.presentation.appointment.host.HostRoomMessageUiState
import com.jigmnnaga.presentation.appointment.participant.ParticipantRoomMessageUiState

@StringRes
fun ChatNotice.toMessageResource(): Int = when (this) {
    ChatNotice.LOGIN_REQUIRED -> R.string.chat_notice_login_required
    ChatNotice.NOT_PARTICIPANT -> R.string.chat_notice_not_participant
    ChatNotice.LOAD_FAILED -> R.string.chat_notice_load_failed
    ChatNotice.SEND_FAILED -> R.string.chat_notice_send_failed
    ChatNotice.MESSAGE_TOO_LONG -> R.string.chat_notice_too_long
}

fun ChatMessageUiState.toHostRoomMessage() = HostRoomMessageUiState(
    id = id,
    senderName = senderName,
    body = body,
    timeLabel = timeLabel,
    isMine = isMine,
    unreadCount = unreadCount,
    isSystem = isSystem
)

fun ChatMessageUiState.toParticipantRoomMessage() = ParticipantRoomMessageUiState(
    id = id,
    body = body,
    senderName = senderName,
    timeLabel = timeLabel,
    isMine = isMine,
    unreadCount = unreadCount,
    isSystem = isSystem
)
