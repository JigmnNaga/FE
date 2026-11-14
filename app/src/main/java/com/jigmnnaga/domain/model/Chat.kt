package com.jigmnnaga.domain.model

enum class ChatMessageType {
    TEXT,

    /** 서버가 약속방에 남기는 안내 메시지. 보낸 사람이 없다. */
    SYSTEM
}

/**
 * 약속방 채팅 메시지. 날짜·시간은 minSdk 24에서 java.time을 쓸 수 없어 서버 문자열
 * (`yyyy-MM-dd'T'HH:mm:ss`) 그대로 둔다. [unreadCount]는 아직 읽지 않은 사람 수이고 안내 메시지는 null이다.
 */
data class ChatMessage(
    val id: Long,
    val type: ChatMessageType,
    val senderId: Long?,
    val senderNickname: String?,
    val content: String,
    val sentAt: String?,
    val unreadCount: Int?
)

/** 이전 메시지 한 페이지. [messages]는 오래된 순이고, [nextCursor]를 넘기면 더 이전 메시지를 받는다. */
data class ChatPage(
    val messages: List<ChatMessage>,
    val myLastReadMessageId: Long?,
    val nextCursor: Long?,
    val hasNext: Boolean
)

/** 누군가 메시지를 읽었다는 실시간 알림. */
data class ChatReadEvent(
    val memberId: Long,
    val previousLastReadMessageId: Long?,
    val lastReadMessageId: Long
)

/** 채팅 REST 호출 결과. */
sealed interface ChatResult<out T> {
    data class Success<T>(val value: T) : ChatResult<T>
    data object LoginRequired : ChatResult<Nothing>

    /** 약속에 참여한 멤버가 아니거나 약속이 없을 때. */
    data object NotParticipant : ChatResult<Nothing>
    data class Failure(val retryable: Boolean) : ChatResult<Nothing>
}
