package com.jigmnnaga.core.websocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StompFrameTest {

    @Test
    fun encode_writesCommandHeadersBodyAndNul() {
        val frame = StompFrame(
            command = StompFrame.SEND,
            headers = mapOf("destination" to "/pub/appointments/1/messages"),
            body = "{\"content\":\"안녕\"}"
        )

        assertEquals(
            "SEND\ndestination:/pub/appointments/1/messages\n\n{\"content\":\"안녕\"}\u0000",
            frame.encode()
        )
    }

    @Test
    fun encode_connectHeadersAreNotEscaped() {
        val frame = StompFrame(StompFrame.CONNECT, mapOf("Authorization" to "Bearer a:b"))

        assertEquals("CONNECT\nAuthorization:Bearer a:b\n\n\u0000", frame.encode())
    }

    @Test
    fun encode_escapesHeaderSpecialCharactersOfOtherFrames() {
        val frame = StompFrame(StompFrame.SEND, mapOf("k:1" to "a\nb\\c"))

        assertEquals("SEND\nk\\c1:a\\nb\\\\c\n\n\u0000", frame.encode())
    }

    @Test
    fun parse_readsMessageFrame() {
        val text = "MESSAGE\nsubscription:sub-0\ndestination:/sub/appointments/1\n\n{\"a\":1}\u0000"

        val frame = StompFrame.parse(text)

        assertEquals(StompFrame.MESSAGE, frame?.command)
        assertEquals("sub-0", frame?.headers?.get("subscription"))
        assertEquals("/sub/appointments/1", frame?.headers?.get("destination"))
        assertEquals("{\"a\":1}", frame?.body)
    }

    @Test
    fun parse_unescapesHeadersAndKeepsColonInValue() {
        val frame = StompFrame.parse("MESSAGE\nmessage:a\\cb\\nc\n\n\u0000")

        assertEquals("a:b\nc", frame?.headers?.get("message"))
    }

    @Test
    fun parse_connectedFrameHeadersStayRaw() {
        val frame = StompFrame.parse("CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\u0000")

        assertEquals("1.2", frame?.headers?.get("version"))
        assertEquals("0,0", frame?.headers?.get("heart-beat"))
    }

    @Test
    fun parse_handlesCrLfAndFrameWithoutBody() {
        val frame = StompFrame.parse("CONNECTED\r\nversion:1.2\r\n\r\n\u0000")

        assertEquals(StompFrame.CONNECTED, frame?.command)
        assertEquals("1.2", frame?.headers?.get("version"))
        assertEquals("", frame?.body)
    }

    @Test
    fun parse_usesFirstValueOfRepeatedHeader() {
        val frame = StompFrame.parse("MESSAGE\nfoo:first\nfoo:second\n\n\u0000")

        assertEquals("first", frame?.headers?.get("foo"))
    }

    @Test
    fun parse_heartbeatAndBlankReturnNull() {
        assertNull(StompFrame.parse("\n"))
        assertNull(StompFrame.parse(""))
        assertNull(StompFrame.parse("\u0000"))
    }

    @Test
    fun encodeThenParse_roundTrips() {
        val original = StompFrame(
            StompFrame.SUBSCRIBE,
            mapOf("id" to "sub-1", "destination" to "/sub/appointments/3"),
            body = ""
        )

        assertEquals(original, StompFrame.parse(original.encode()))
    }
}
