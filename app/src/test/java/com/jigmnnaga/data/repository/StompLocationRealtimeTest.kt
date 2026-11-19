package com.jigmnnaga.data.repository

import com.jigmnnaga.core.websocket.StompClient
import com.jigmnnaga.core.websocket.StompFrame
import com.jigmnnaga.core.websocket.StompTransport
import com.jigmnnaga.core.websocket.StompTransportFactory
import com.jigmnnaga.core.websocket.StompTransportListener
import com.jigmnnaga.domain.model.LocationShareEnd
import com.jigmnnaga.domain.model.LocationShareEndReason
import com.jigmnnaga.domain.model.LocationShareError
import com.jigmnnaga.domain.model.MemberArrival
import com.jigmnnaga.domain.model.MemberDepartureStatus
import com.jigmnnaga.domain.model.MemberEtaUpdate
import com.jigmnnaga.domain.model.MemberLocationUpdate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StompLocationRealtimeTest {

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

    private fun TestScope.realtime(factory: FakeFactory) = StompLocationRealtime(
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

    /** [destination]을 구독한 id로 [body]를 서버가 보낸 것처럼 전달한다. */
    private fun FakeFactory.deliver(destination: String, body: String) {
        val subscribe = transports.last().sent.last {
            it.command == StompFrame.SUBSCRIBE && it.headers["destination"] == destination
        }
        listeners.last().onText(
            StompFrame(StompFrame.MESSAGE, mapOf("subscription" to subscribe.headers.getValue("id")), body).encode()
        )
    }

    @Test
    fun locations_decodeServerPayload() = runTest {
        val factory = FakeFactory()
        val realtime = realtime(factory)
        val received = mutableListOf<MemberLocationUpdate>()
        backgroundScope.launch { realtime.locations(7L).collect { received += it } }
        realtime.connect()
        runCurrent()
        factory.serverConnected()

        factory.deliver(
            "/sub/appointments/7/location",
            """{"memberId":3,"latitude":37.4979,"longitude":127.0276,"departureStatus":"DEPARTED","updatedAt":"2026-11-19T16:30:00","extra":1}"""
        )
        factory.deliver(
            "/sub/appointments/7/location",
            """{"memberId":4,"latitude":37.5,"longitude":127.0,"departureStatus":"SOMETHING_NEW"}"""
        )
        factory.deliver("/sub/appointments/7/location", "not json")
        runCurrent()

        assertEquals(2, received.size)
        assertEquals(3L, received[0].memberId)
        assertEquals(37.4979, received[0].latitude, 0.0)
        assertEquals(127.0276, received[0].longitude, 0.0)
        assertEquals(MemberDepartureStatus.DEPARTED, received[0].departureStatus)
        assertEquals("2026-11-19T16:30:00", received[0].updatedAt)
        assertEquals(MemberDepartureStatus.UNKNOWN, received[1].departureStatus)
        assertNull(received[1].updatedAt)
    }

    @Test
    fun etas_decodeServerPayload() = runTest {
        val factory = FakeFactory()
        val realtime = realtime(factory)
        val received = mutableListOf<MemberEtaUpdate>()
        backgroundScope.launch { realtime.etas(7L).collect { received += it } }
        realtime.connect()
        runCurrent()
        factory.serverConnected()

        factory.deliver(
            "/sub/appointments/7/eta",
            """{"memberId":3,"estimatedArrivalTime":"2026-11-19T16:55:00","remainingMinutes":12,"calculatedAt":"2026-11-19T16:43:00","expectedLatenessMinutes":null,"isExpectedLate":false}"""
        )
        runCurrent()

        val eta = received.single()
        assertEquals(3L, eta.memberId)
        assertEquals("2026-11-19T16:55:00", eta.estimatedArrivalTime)
        assertEquals(12, eta.remainingMinutes)
        assertNull(eta.expectedLatenessMinutes)
        assertEquals(false, eta.isExpectedLate)
    }

    @Test
    fun arrivalsAndSharingEnds_decodeServerPayload() = runTest {
        val factory = FakeFactory()
        val realtime = realtime(factory)
        val arrivals = mutableListOf<MemberArrival>()
        val ends = mutableListOf<LocationShareEnd>()
        backgroundScope.launch { realtime.arrivals(7L).collect { arrivals += it } }
        backgroundScope.launch { realtime.sharingEnds(7L).collect { ends += it } }
        realtime.connect()
        runCurrent()
        factory.serverConnected()

        factory.deliver("/sub/appointments/7/arrival", """{"memberId":3,"arrivedAt":"2026-11-19T16:58:00"}""")
        factory.deliver(
            "/sub/appointments/7/location-end",
            """{"memberId":3,"reason":"ARRIVED","endedAt":"2026-11-19T16:58:01"}"""
        )
        factory.deliver("/sub/appointments/7/location-end", """{"memberId":4,"reason":"USER_REQUEST"}""")
        factory.deliver("/sub/appointments/7/location-end", """{"memberId":5,"reason":"NEW_REASON"}""")
        runCurrent()

        assertEquals(MemberArrival(3L, "2026-11-19T16:58:00"), arrivals.single())
        assertEquals(
            listOf(LocationShareEndReason.ARRIVED, LocationShareEndReason.USER_REQUEST, LocationShareEndReason.UNKNOWN),
            ends.map { it.reason }
        )
    }

    @Test
    fun errors_mapLocationCodesAndIgnoreOthers() = runTest {
        val factory = FakeFactory()
        val realtime = realtime(factory)
        val received = mutableListOf<LocationShareError>()
        backgroundScope.launch { realtime.errors().collect { received += it } }
        realtime.connect()
        runCurrent()
        factory.serverConnected()

        listOf(
            "NOT_DEPARTED",
            "LOCATION_SHARING_STOPPED",
            "LOCATION_UPDATE_TOO_FREQUENT",
            "APPOINTMENT_CANCELED",
            "INVALID_CHAT_MESSAGE"
        ).forEach { code ->
            factory.deliver("/user/queue/errors", """{"success":false,"code":"$code","message":"m","data":null}""")
        }
        factory.deliver("/user/queue/errors", "not json")
        runCurrent()

        assertEquals(
            listOf(
                LocationShareError.NOT_DEPARTED,
                LocationShareError.SHARING_STOPPED,
                LocationShareError.TOO_FREQUENT,
                LocationShareError.OTHER
            ),
            received
        )
    }

    @Test
    fun sendLocation_usesPublishDestinationWithJsonBody() = runTest {
        val factory = FakeFactory()
        val realtime = realtime(factory)
        assertFalse(realtime.sendLocation(7L, 37.5, 127.0))

        realtime.connect()
        runCurrent()
        factory.serverConnected()

        assertTrue(realtime.sendLocation(7L, 37.4979, 127.0276))
        val frame = factory.transports[0].sent.last { it.command == StompFrame.SEND }
        assertEquals("/pub/appointments/7/location", frame.headers["destination"])
        assertEquals("""{"latitude":37.4979,"longitude":127.0276}""", frame.body)
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
