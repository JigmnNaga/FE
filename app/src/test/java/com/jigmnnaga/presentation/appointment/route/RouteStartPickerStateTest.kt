package com.jigmnnaga.presentation.appointment.route

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteStartPickerStateTest {
    private val home = RouteStartLocation(
        id = "basic-1",
        kind = RouteStartKind.BASIC,
        name = "집",
        detail = "서울 강남구 테헤란로 123",
        latitude = 37.5,
        longitude = 127.0
    )
    private val noCoordinates = RouteStartLocation(id = "search-9", kind = RouteStartKind.SEARCH, name = "좌표 없음")

    private fun selecting() = RouteSampleData.selecting

    @Test
    fun `경로 선택 단계에서만 출발 위치 선택 화면을 연다`() {
        assertTrue(selecting().openStartPicker().startPicker.isOpen)
        assertFalse(RouteSampleData.overview.openStartPicker().startPicker.isOpen)
    }

    @Test
    fun `후보를 고르지 않으면 출발하기를 누를 수 없다`() {
        val opened = selecting().openStartPicker()
        assertFalse(opened.startPicker.canConfirm)
        assertNull(opened.confirmStartLocation().startLocation)
        assertTrue(opened.confirmStartLocation().startPicker.isOpen)
    }

    @Test
    fun `좌표가 없는 후보는 고를 수 없다`() {
        val state = selecting().openStartPicker().selectStartCandidate(noCoordinates)
        assertNull(state.startPicker.selected)
    }

    @Test
    fun `후보를 고르고 출발하기를 누르면 출발 위치가 정해지고 화면이 닫힌다`() {
        val state = selecting().openStartPicker().selectStartCandidate(home).confirmStartLocation()
        assertEquals(home, state.startLocation)
        assertFalse(state.startPicker.isOpen)
    }

    @Test
    fun `선택 화면을 닫으면 출발 위치는 그대로다`() {
        val confirmed = selecting().openStartPicker().selectStartCandidate(home).confirmStartLocation()
        val reopened = confirmed.openStartPicker()
        assertTrue(reopened.startPicker.isSelected("basic-1"))
        val closed = reopened.closeStartPicker()
        assertEquals(home, closed.startLocation)
        assertFalse(closed.startPicker.isOpen)
    }
}
