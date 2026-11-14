package com.jigmnnaga.core.designsystem.component.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMapViewTest {

    @Test
    fun `앱 키가 비어 있으면 지도를 쓸 수 없다`() {
        assertFalse(isMapSupported(""))
        assertFalse(isMapSupported("   "))
    }

    @Test
    fun `앱 키가 있으면 지도를 쓸 수 있다`() {
        assertTrue(isMapSupported("test-key"))
    }
}
