package com.jigmnnaga.data.repository

import com.jigmnnaga.core.websocket.StompClient
import com.jigmnnaga.core.websocket.StompFrame
import com.jigmnnaga.core.websocket.StompTransport
import com.jigmnnaga.core.websocket.StompTransportFactory
import com.jigmnnaga.core.websocket.StompTransportListener
import com.jigmnnaga.domain.model.ChatMessageType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StompChatRealtimeTest {

    private val json = Json { ignoreUnknownKeys = true }

    private class FakeTransport : StompTransport {
        val sent = mutableListOf<StompFrame>()
        var closed = false
        override fun send(text: String): Boolean {
            StompFrame.parse(text)?.let { sent += it }
            return true
        }
        override fun close() { closed = true }
    }

    private class FakeFactory : StompTransportFactory {
        val transports = mutableListOf<FakeTransport>()
        val listeners = mutableListOf<StompTransportListener>()
        override fun open(url: String, listener: StompTransportListener): StompTransport {
            val transport = FakeTransport()
            transports += transport
            listeners += listener
            return transport
        }
    }

    private fun TestScope.realtime(factory: FakeFactory) = StompChatRealtime(
        stomp = StompClient(
            serverUrl = "wss://example.com/ws",
            accessToken = { "token" },
            transportFactory = factory,
            scope = backgroundScope,
            retryDelayMillis = { 1_000L }
        ),
        json = json
    )

    private fun FakeFactory.serverConnected() {
        listeners.last().onOpen(transports.last())
        listeners.last().onText(StompFrame(StompFrame.CONNECTED).encode())
    }

    @Test
    fun messages_decodeServerPayload() = runTest {
        val factory = FakeFactory()
        val realtime = realtime(factory)
        val received = mutableListOf<com.jigmnnaga.domain.model.ChatMessage>()
        backgroundScope.launch { realtime.messages(7L).collect { received += it } }
        realtime.connect()
        runCurrent()
        factory.serverConnected()
        val subscribe = factory.transports[0].sent.last { it.command == StompFrame.SUBSCRIBE }
        assertEquals("/sub/appointments/7", subscribe.headers["destination"])

        factory.listeners[0].onText(
            StompFrame(
                StompFrame.MESSAGE,
                mapOf("subscription" to subscribe.headers.getValue("id")),
                """{"messageId":11,"appointmentId":7,"type":"TEXT","senderId":3,"senderNickname":"태희","content":"안녕","sentAt":"2026-11-14T13:22:00","unreadCount":2,"extra":1}"""
            ).encode()
        )
        factory.listeners[0].onText(
            StompFrame(StompFrame.MESSAGE, mapOf("subscription" to subscribe.headers.getValue("id")), "not json").encode()
        )
        runCurrent()

        val message = received.single()
        assertEquals(11L, message.id)
        assertEquals(ChatMessageType.TEXT, message.type)
        assertEquals(3L, message.senderId)
        assertEquals("안녕", message.content)
        assertEquals(2, message.unreadCount)
    }

    @Test
    fun readEvents_decodeServerPayload() = runTest {
        val factory = FakeFactory()
        val realtime = realtime(factory)
        val received = mutableListOf<com.jigmnnaga.domain.model.ChatReadEvent>()
        backgroundScope.launch { realtime.readEvents(7L).collect { received += it } }
        realtime.connect()
        runCurrent()
        factory.serverConnected()
        val subscribe = factory.transports[0].sent.last { it.command == StompFrame.SUBSCRIBE }
        assertEquals("/sub/appointments/7/read", subscribe.headers["destination"])

        factory.listeners[0].onText(
            StompFrame(
                StompFrame.MESSAGE,
                mapOf("subscription" to subscribe.headers.getValue("id")),
                """{"memberId":3,"previousLastReadMessageId":null,"lastReadMessageId":9}"""
            ).encode()
        )
        runCurrent()

        val event = received.single()
        assertEquals(3L, event.memberId)
        assertEquals(null, event.previousLastReadMessageId)
        assertEquals(9L, event.lastReadMessageId)
    }

    @Test
    fun send_usesPublishDestinationsWithJsonBodies() = runTest {
        val factory = FakeFactory()
        val realtime = realtime(factory)
        assertFalse(realtime.sendMessage(7L, "안녕"))

        realtime.connect()
        runCurrent()
        factory.serverConnected()

        assertTrue(realtime.sendMessage(7L, "안녕"))
        assertTrue(realtime.sendRead(7L, 12L))
        val frames = factory.transports[0].sent.filter { it.command == StompFrame.SEND }
        assertEquals("/pub/appointments/7/messages", frames[0].headers["destination"])
        assertEquals("""{"content":"안녕"}""", frames[0].body)
        assertEquals("/pub/appointments/7/read", frames[1].headers["destination"])
        assertEquals("""{"lastReadMessageId":12}""", frames[1].body)
    }

    @Test
    fun isConnected_followsConnectionState() = runTest {
        val factory = FakeFactory()
        val realtime = realtime(factory)
        val states = mutableListOf<Boolean>()
        backgroundScope.launch { realtime.isConnected.toList(states) }
        runCurrent()

        realtime.connect()
        runCurrent()
        factory.serverConnected()
        runCurrent()

        assertEquals(listOf(false, true), states)
    }

    @Test
    fun disconnect_closesOnlyWhenLastUserLeaves() = runTest {
        val factory = FakeFactory()
        val realtime = realtime(factory)
        realtime.connect()
        realtime.connect()
        runCurrent()
        factory.serverConnected()

        realtime.disconnect()
        assertFalse(factory.transports[0].closed)

        realtime.disconnect()
        assertTrue(factory.transports[0].closed)

        // 짝이 맞지 않는 disconnect는 무시한다.
        realtime.disconnect()
        assertEquals(1, factory.transports.size)
        assertEquals(false, realtime.isConnected.first())
    }
}
