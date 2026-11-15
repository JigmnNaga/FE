package com.jigmnnaga.presentation.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeUiStateTest {

    private val sampleUpcoming = listOf(
        MockAppointment(id = "u1", title = "예정 약속", dateTime = "10월 24일", location = "강남", participants = emptyList(), participantCount = 2)
    )
    private val samplePast = listOf(
        MockAppointment(id = "p1", title = "지난 약속", dateTime = "9월 10일", location = "홍대", participants = emptyList(), participantCount = 3)
    )
    private val upcomingError = HomeListError(retryable = true)
    private val pastError = HomeListError(retryable = false)

    // ── currentAppointments ───────────────────────────────────────────────────

    @Test
    fun `selectedTab UPCOMING이면 currentAppointments는 upcomingAppointments를 반환한다`() {
        val state = HomeUiState(
            selectedTab = HomeTab.UPCOMING,
            upcomingAppointments = sampleUpcoming,
            pastAppointments = samplePast
        )

        assertEquals(sampleUpcoming, state.currentAppointments)
    }

    @Test
    fun `selectedTab PAST이면 currentAppointments는 pastAppointments를 반환한다`() {
        val state = HomeUiState(
            selectedTab = HomeTab.PAST,
            upcomingAppointments = sampleUpcoming,
            pastAppointments = samplePast
        )

        assertEquals(samplePast, state.currentAppointments)
    }

    @Test
    fun `탭이 변경되면 currentAppointments가 해당 탭의 목록으로 바뀐다`() {
        val base = HomeUiState(
            selectedTab = HomeTab.UPCOMING,
            upcomingAppointments = sampleUpcoming,
            pastAppointments = samplePast
        )

        val switched = base.copy(selectedTab = HomeTab.PAST)

        assertEquals(sampleUpcoming, base.currentAppointments)
        assertEquals(samplePast, switched.currentAppointments)
    }

    // ── currentTabError ───────────────────────────────────────────────────────

    @Test
    fun `selectedTab UPCOMING이면 currentTabError는 upcomingError를 반환한다`() {
        val state = HomeUiState(
            selectedTab = HomeTab.UPCOMING,
            upcomingError = upcomingError,
            pastError = pastError
        )

        assertEquals(upcomingError, state.currentTabError)
    }

    @Test
    fun `selectedTab PAST이면 currentTabError는 pastError를 반환한다`() {
        val state = HomeUiState(
            selectedTab = HomeTab.PAST,
            upcomingError = upcomingError,
            pastError = pastError
        )

        assertEquals(pastError, state.currentTabError)
    }

    @Test
    fun `오류가 없으면 currentTabError는 null이다`() {
        val state = HomeUiState(selectedTab = HomeTab.UPCOMING)

        assertNull(state.currentTabError)
    }

    @Test
    fun `반대 탭 오류는 현재 탭 currentTabError에 포함되지 않는다`() {
        val state = HomeUiState(
            selectedTab = HomeTab.UPCOMING,
            upcomingError = null,
            pastError = pastError
        )

        assertNull(state.currentTabError)
    }

    @Test
    fun `PAST 탭에서 upcomingError만 있으면 currentTabError는 null이다`() {
        val state = HomeUiState(
            selectedTab = HomeTab.PAST,
            upcomingError = upcomingError,
            pastError = null
        )

        assertNull(state.currentTabError)
    }
}
