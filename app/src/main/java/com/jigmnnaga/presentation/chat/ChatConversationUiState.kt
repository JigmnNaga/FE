package com.jigmnnaga.presentation.chat

/** UI data supplied by the caller; this model does not send or persist messages. */
data class ChatConversationUiState(
    val messages: List<ChatMessageUiState> = emptyList(),
    val systemNotice: String? = null,
    val draftMessage: String = ""
)

data class ChatMessageUiState(
    val id: String,
    val body: String,
    val isMine: Boolean,
    val senderName: String? = null,
    val timeLabel: String? = null
)
