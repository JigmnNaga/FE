package com.jigmnnaga.core.navigation

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LoginReturnRegistryTest {

    @After
    fun tearDown() {
        LoginReturnRegistry.consume()
    }

    @Test
    fun `맡겨 둔 일이 없으면 null이다`() {
        assertNull(LoginReturnRegistry.consume())
    }

    @Test
    fun `맡겨 둔 일을 꺼내 실행할 수 있다`() {
        var count = 0
        LoginReturnRegistry.request { count++ }

        val action = LoginReturnRegistry.consume()

        assertNotNull(action)
        action?.invoke()
        assertEquals(1, count)
    }

    @Test
    fun `한 번 꺼내면 다시 꺼낼 수 없다`() {
        LoginReturnRegistry.request { }

        LoginReturnRegistry.consume()

        assertNull(LoginReturnRegistry.consume())
    }

    @Test
    fun `새로 맡기면 이전 일을 덮어쓴다`() {
        var first = 0
        var second = 0
        LoginReturnRegistry.request { first++ }
        LoginReturnRegistry.request { second++ }

        LoginReturnRegistry.consume()?.invoke()

        assertEquals(0, first)
        assertEquals(1, second)
    }
}
