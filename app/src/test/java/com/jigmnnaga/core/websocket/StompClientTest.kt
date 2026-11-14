package com.jigmnnaga.core.websocket

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StompClientTest {

    private class FakeTransport : StompTransport {
        val sent = mutableListOf<StompFrame>()
        var closed = false
        var acceptSend = true

        override fun send(text: String): Boolean {
            if (!acceptSend) return false
            StompFrame.parse(text)?.let { sent += it }
            return true
        }

        override fun close() {
            closed = true
        }

        fun commands() = sent.map { it.command }
    }

    private class FakeFactory : StompTransportFactory {
        val urls = mutableListOf<String>()
        val transports = mutableListOf<FakeTransport>()
        val listeners = mutableListOf<StompTransportListener>()

        override fun open(url: String, listener: StompTransportListener): StompTransport {
            urls += url
            val transport = FakeTransport()
            transports += transport
            listeners += listener
            return transport
        }

        /** 서버와 WebSocket이 연결된 상태를 흉내 낸다. */
        fun opened(index: Int = transports.lastIndex) = listeners[index].onOpen(transports[index])

        fun serverSays(frame: StompFrame, index: Int = listeners.lastIndex) =
            listeners[index].onText(frame.encode())
    }

    private fun TestScope.client(
        factory: FakeFactory,
        token: () -> String? = { "token-1" },
        tokens: kotlinx.coroutines.flow.Flow<String?>? = null
    ) = StompClient(
        serverUrl = "wss://example.com/ws",
        accessToken = { token() },
        transportFactory = factory,
        scope = backgroundScope,
        retryDelayMillis = { attempt -> 1_000L * (attempt + 1) },
        accessTokens = tokens
    )

    private fun FakeFactory.connectedServer(index: Int = transports.lastIndex) {
        opened(index)
        serverSays(StompFrame(StompFrame.CONNECTED, mapOf("version" to "1.2")), index)
    }

    @Test
    fun connect_sendsStompConnectWithBearerTokenAndBecomesConnected() = runTest {
        val factory = FakeFactory()
        val client = client(factory)

        client.connect()
        runCurrent()
        assertEquals(StompConnectionState.CONNECTING, client.connectionState.value)
        assertEquals(listOf("wss://example.com/ws"), factory.urls)

        factory.opened()
        val connect = factory.transports[0].sent.single()
        assertEquals(StompFrame.CONNECT, connect.command)
        assertEquals("Bearer token-1", connect.headers["Authorization"])
        assertEquals("1.2", connect.headers["accept-version"])
        assertEquals(StompConnectionState.CONNECTING, client.connectionState.value)

        factory.serverSays(StompFrame(StompFrame.CONNECTED, mapOf("version" to "1.2")))
        assertEquals(StompConnectionState.CONNECTED, client.connectionState.value)
    }

    @Test
    fun connect_calledTwice_opensOnlyOneConnection() = runTest {
        val factory = FakeFactory()
        val client = client(factory)

        client.connect()
        client.connect()
        runCurrent()

        assertEquals(1, factory.transports.size)
    }

    @Test
    fun connect_withoutToken_reportsLoginRequiredAndDoesNotOpen() = runTest {
        val factory = FakeFactory()
        val client = client(factory, token = { null })

        client.connect()
        runCurrent()

        assertEquals(StompConnectionState.LOGIN_REQUIRED, client.connectionState.value)
        assertTrue(factory.transports.isEmpty())
    }

    @Test
    fun subscribe_beforeConnected_subscribesOnConnectedAndDeliversMessages() = runTest {
        val factory = FakeFactory()
        val client = client(factory)
        val received = mutableListOf<StompMessage>()
        backgroundScope.launch { client.subscribe("/sub/appointments/1").collect { received += it } }
        runCurrent()
        client.connect()
        runCurrent()

        factory.connectedServer()

        val subscribe = factory.transports[0].sent.last()
        assertEquals(StompFrame.SUBSCRIBE, subscribe.command)
        assertEquals("/sub/appointments/1", subscribe.headers["destination"])

        factory.serverSays(
            StompFrame(
                StompFrame.MESSAGE,
                mapOf("subscription" to subscribe.headers.getValue("id"), "destination" to "/sub/appointments/1"),
                "{\"content\":\"hi\"}"
            )
        )
        runCurrent()

        assertEquals(1, received.size)
        assertEquals("/sub/appointments/1", received[0].destination)
        assertEquals("{\"content\":\"hi\"}", received[0].body)
    }

    @Test
    fun subscribe_whileConnected_subscribesImmediately_andRoutesBySubscriptionId() = runTest {
        val factory = FakeFactory()
        val client = client(factory)
        client.connect()
        runCurrent()
        factory.connectedServer()

        val chat = mutableListOf<String>()
        val read = mutableListOf<String>()
        backgroundScope.launch { client.subscribe("/sub/appointments/1").collect { chat += it.body } }
        backgroundScope.launch { client.subscribe("/sub/appointments/1/read").collect { read += it.body } }
        runCurrent()

        val subscribes = factory.transports[0].sent.filter { it.command == StompFrame.SUBSCRIBE }
        assertEquals(2, subscribes.size)
        val chatId = subscribes[0].headers.getValue("id")
        val readId = subscribes[1].headers.getValue("id")

        factory.serverSays(StompFrame(StompFrame.MESSAGE, mapOf("subscription" to readId), "R"))
        factory.serverSays(StompFrame(StompFrame.MESSAGE, mapOf("subscription" to chatId), "C"))
        runCurrent()

        assertEquals(listOf("C"), chat)
        assertEquals(listOf("R"), read)
    }

    @Test
    fun subscribe_cancelled_sendsUnsubscribe() = runTest {
        val factory = FakeFactory()
        val client = client(factory)
        client.connect()
        runCurrent()
        factory.connectedServer()
        val job = backgroundScope.launch { client.subscribe("/sub/appointments/1").collect { } }
        runCurrent()
        val id = factory.transports[0].sent.last { it.command == StompFrame.SUBSCRIBE }.headers.getValue("id")

        job.cancel()
        runCurrent()

        val unsubscribe = factory.transports[0].sent.last()
        assertEquals(StompFrame.UNSUBSCRIBE, unsubscribe.command)
        assertEquals(id, unsubscribe.headers["id"])
    }

    @Test
    fun send_returnsFalseUntilConnected_thenSendsJsonFrame() = runTest {
        val factory = FakeFactory()
        val client = client(factory)
        assertFalse(client.send("/pub/appointments/1/messages", "{}"))

        client.connect()
        runCurrent()
        factory.opened()
        assertFalse(client.send("/pub/appointments/1/messages", "{}"))
        factory.serverSays(StompFrame(StompFrame.CONNECTED))

        assertTrue(client.send("/pub/appointments/1/messages", "{\"content\":\"hi\"}"))
        val frame = factory.transports[0].sent.last()
        assertEquals(StompFrame.SEND, frame.command)
        assertEquals("/pub/appointments/1/messages", frame.headers["destination"])
        assertEquals("application/json", frame.headers["content-type"])
        assertEquals("{\"content\":\"hi\"}", frame.body)
    }

    @Test
    fun connectionFailure_reconnectsWithBackoffAndRestoresSubscriptions() = runTest {
        val factory = FakeFactory()
        val tokens = mutableListOf("token-1", "token-2")
        val client = client(factory, token = { tokens.removeAt(0) })
        backgroundScope.launch { client.subscribe("/sub/appointments/1").collect { } }
        runCurrent()
        client.connect()
        runCurrent()
        factory.connectedServer()

        factory.listeners[0].onFailure(RuntimeException("network down"))
        assertEquals(StompConnectionState.DISCONNECTED, client.connectionState.value)

        advanceTimeBy(999)
        runCurrent()
        assertEquals(1, factory.transports.size)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, factory.transports.size)
        factory.opened(1)
        assertEquals("Bearer token-2", factory.transports[1].sent.single().headers["Authorization"])
        factory.serverSays(StompFrame(StompFrame.CONNECTED), index = 1)

        assertEquals(StompConnectionState.CONNECTED, client.connectionState.value)
        val resubscribe = factory.transports[1].sent.last()
        assertEquals(StompFrame.SUBSCRIBE, resubscribe.command)
        assertEquals("/sub/appointments/1", resubscribe.headers["destination"])
    }

    @Test
    fun repeatedFailures_increaseDelay_andSuccessResetsIt() = runTest {
        val factory = FakeFactory()
        val client = client(factory)
        client.connect()
        runCurrent()

        factory.listeners[0].onFailure(RuntimeException())
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, factory.transports.size)

        factory.listeners[1].onFailure(RuntimeException())
        advanceTimeBy(1_999)
        runCurrent()
        assertEquals(2, factory.transports.size)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(3, factory.transports.size)

        factory.connectedServer(2)
        factory.listeners[2].onClosed()
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(4, factory.transports.size)
    }

    @Test
    fun serverErrorFrame_closesConnectionAndReconnects() = runTest {
        val factory = FakeFactory()
        val client = client(factory)
        client.connect()
        runCurrent()
        factory.opened()

        factory.serverSays(StompFrame(StompFrame.ERROR, mapOf("message" to "unauthorized")))

        assertTrue(factory.transports[0].closed)
        assertEquals(StompConnectionState.DISCONNECTED, client.connectionState.value)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(2, factory.transports.size)
    }

    @Test
    fun disconnect_sendsDisconnect_closes_andStopsReconnecting() = runTest {
        val factory = FakeFactory()
        val client = client(factory)
        client.connect()
        runCurrent()
        factory.connectedServer()

        client.disconnect()

        assertEquals(StompFrame.DISCONNECT, factory.transports[0].sent.last().command)
        assertTrue(factory.transports[0].closed)
        assertEquals(StompConnectionState.DISCONNECTED, client.connectionState.value)

        factory.listeners[0].onClosed()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(1, factory.transports.size)
    }

    @Test
    fun disconnect_duringRetryWait_cancelsTheScheduledReconnect() = runTest {
        val factory = FakeFactory()
        val client = client(factory)
        client.connect()
        runCurrent()
        factory.listeners[0].onFailure(RuntimeException())

        client.disconnect()
        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(1, factory.transports.size)
    }

    @Test
    fun connectAfterDisconnect_opensNewConnection() = runTest {
        val factory = FakeFactory()
        val client = client(factory)
        client.connect()
        runCurrent()
        client.disconnect()

        client.connect()
        runCurrent()

        assertEquals(2, factory.transports.size)
    }

    @Test
    fun staleConnectionEvents_areIgnoredAfterReconnect() = runTest {
        val factory = FakeFactory()
        val client = client(factory)
        client.connect()
        runCurrent()
        factory.connectedServer()
        factory.listeners[0].onFailure(RuntimeException())
        advanceTimeBy(1_000)
        runCurrent()
        factory.connectedServer(1)

        factory.listeners[0].onClosed()

        assertEquals(StompConnectionState.CONNECTED, client.connectionState.value)
        assertEquals(2, factory.transports.size)
    }

    @Test
    fun tokenBecomingEmpty_disconnects() = runTest {
        val factory = FakeFactory()
        val tokens = MutableStateFlow<String?>("token-1")
        val client = client(factory, tokens = tokens)
        runCurrent()
        client.connect()
        runCurrent()
        factory.connectedServer()
        assertEquals(StompConnectionState.CONNECTED, client.connectionState.value)

        tokens.value = null
        runCurrent()

        assertEquals(StompConnectionState.DISCONNECTED, client.connectionState.value)
        assertTrue(factory.transports[0].closed)
    }

    @Test
    fun urlAndRetryHelpers() {
        assertEquals("wss://api.example.com/ws", stompServerUrl("https://api.example.com/"))
        assertEquals("ws://10.0.2.2:8080/ws", stompServerUrl("http://10.0.2.2:8080"))
        assertEquals(1_000L, defaultRetryDelayMillis(0))
        assertEquals(2_000L, defaultRetryDelayMillis(1))
        assertEquals(30_000L, defaultRetryDelayMillis(5))
        assertEquals(30_000L, defaultRetryDelayMillis(50))
    }
}
