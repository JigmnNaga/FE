package com.jigmnnaga.presentation.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatTimeTest {

    @Test
    fun formatsAfternoonAndMorning() {
        assertEquals("오후 1:22", formatChatTime("2026-11-14T13:22:05"))
        assertEquals("오전 9:05", formatChatTime("2026-11-14T09:05:00"))
    }

    @Test
    fun noonAndMidnightUseTwelve() {
        assertEquals("오후 12:00", formatChatTime("2026-11-14T12:00:00"))
        assertEquals("오전 12:30", formatChatTime("2026-11-14T00:30:00"))
    }

    @Test
    fun ignoresFractionalSeconds() {
        assertEquals("오후 11:59", formatChatTime("2026-11-14T23:59:59.123456"))
    }

    @Test
    fun returnsNullWhenUnreadable() {
        assertNull(formatChatTime(null))
        assertNull(formatChatTime(""))
        assertNull(formatChatTime("2026-11-14"))
        assertNull(formatChatTime("2026-11-14T25:00:00"))
        assertNull(formatChatTime("2026-11-14Tab:cd:00"))
    }
}
