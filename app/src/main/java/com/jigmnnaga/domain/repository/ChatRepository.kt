package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.ChatMessage
import com.jigmnnaga.domain.model.ChatPage
import com.jigmnnaga.domain.model.ChatReadEvent
import com.jigmnnaga.domain.model.ChatResult
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    /** [cursor]를 비우면 가장 최근 메시지부터, 넘기면 그 메시지보다 이전 메시지를 [size]개 받는다. */
    suspend fun getMessages(appointmentId: Long, cursor: Long? = null, size: Int = DEFAULT_PAGE_SIZE): ChatResult<ChatPage>

    /** 마지막으로 읽은 메시지를 서버에 알린다. */
    suspend fun markAsRead(appointmentId: Long, lastReadMessageId: Long): ChatResult<Unit>

    companion object {
        const val DEFAULT_PAGE_SIZE = 30
    }
}

/** 실시간(STOMP) 채팅 연결. [connect]와 [disconnect]는 짝으로 부르고, 마지막 사용자가 끊을 때 실제로 연결을 닫는다. */
interface ChatRealtime {
    val isConnected: Flow<Boolean>

    fun connect()
    fun disconnect()

    /** 모으는 동안 약속방의 새 메시지를 받는다. 연결 전에 모아도 연결되면 받기 시작한다. */
    fun messages(appointmentId: Long): Flow<ChatMessage>
    fun readEvents(appointmentId: Long): Flow<ChatReadEvent>

    /** 연결되어 있지 않거나 보내지 못하면 false. */
    fun sendMessage(appointmentId: Long, content: String): Boolean
    fun sendRead(appointmentId: Long, lastReadMessageId: Long): Boolean
}

/** 로그인한 사용자의 멤버 ID. 채팅에서 내 메시지를 가르는 데 쓴다. 로그인하지 않았으면 null. */
fun interface MyMemberIdProvider {
    suspend fun myMemberId(): Long?
}
