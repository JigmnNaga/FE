package com.jigmnnaga.data.repository

import com.jigmnnaga.core.websocket.StompClient
import com.jigmnnaga.core.websocket.StompConnectionState
import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.remote.dto.ChatMessageDto
import com.jigmnnaga.data.remote.dto.ChatReadDto
import com.jigmnnaga.data.remote.dto.ChatReadRequestDto
import com.jigmnnaga.data.remote.dto.ChatSendRequestDto
import com.jigmnnaga.domain.model.ChatMessage
import com.jigmnnaga.domain.model.ChatReadEvent
import com.jigmnnaga.domain.repository.ChatRealtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.Json

/**
 * 공통 [StompClient] 위에서 약속방 채팅 목적지를 구독하고 보낸다.
 * 연결은 앱 전체가 하나라서, [connect]를 부른 만큼 [disconnect]가 불려야 실제로 끊는다.
 */
class StompChatRealtime(
    private val stomp: StompClient,
    private val json: Json
) : ChatRealtime {

    private val lock = Any()
    private var users = 0

    override val isConnected: Flow<Boolean> =
        stomp.connectionState.map { it == StompConnectionState.CONNECTED }.distinctUntilChanged()

    override fun connect() {
        synchronized(lock) { users++ }
        stomp.connect()
    }

    override fun disconnect() {
        val last = synchronized(lock) {
            if (users == 0) return
            users--
            users == 0
        }
        if (last) stomp.disconnect()
    }

    override fun messages(appointmentId: Long): Flow<ChatMessage> =
        stomp.subscribe("/sub/appointments/$appointmentId").mapNotNull { message ->
            decode<ChatMessageDto>(message.body)?.toDomain()
        }

    override fun readEvents(appointmentId: Long): Flow<ChatReadEvent> =
        stomp.subscribe("/sub/appointments/$appointmentId/read").mapNotNull { message ->
            decode<ChatReadDto>(message.body)?.toDomain()
        }

    override fun sendMessage(appointmentId: Long, content: String): Boolean = stomp.send(
        destination = "/pub/appointments/$appointmentId/messages",
        body = json.encodeToString(ChatSendRequestDto.serializer(), ChatSendRequestDto(content))
    )

    override fun sendRead(appointmentId: Long, lastReadMessageId: Long): Boolean = stomp.send(
        destination = "/pub/appointments/$appointmentId/read",
        body = json.encodeToString(ChatReadRequestDto.serializer(), ChatReadRequestDto(lastReadMessageId))
    )

    private inline fun <reified T> decode(body: String): T? = try {
        json.decodeFromString<T>(body)
    } catch (e: Exception) {
        null
    }
}
