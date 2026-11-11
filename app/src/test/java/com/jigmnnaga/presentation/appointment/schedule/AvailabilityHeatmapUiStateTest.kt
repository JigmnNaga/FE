package com.jigmnnaga.presentation.appointment.schedule

import org.junit.Assert.assertEquals
import org.junit.Test

class AvailabilityHeatmapUiStateTest {
    @Test
    fun `count maps to availability level`() {
        assertEquals(AvailabilityLevel.NONE, AvailabilityLevel.fromCount(0))
        assertEquals(AvailabilityLevel.NONE, AvailabilityLevel.fromCount(-1))
        assertEquals(AvailabilityLevel.LOW, AvailabilityLevel.fromCount(1))
        assertEquals(AvailabilityLevel.MEDIUM, AvailabilityLevel.fromCount(2))
        assertEquals(AvailabilityLevel.HIGH, AvailabilityLevel.fromCount(3))
        assertEquals(AvailabilityLevel.HIGH, AvailabilityLevel.fromCount(7))
    }

    @Test
    fun `level is read from row and column`() {
        val state = heatmapState(listOf(listOf(0, 1), listOf(2, 3)))

        assertEquals(AvailabilityLevel.NONE, state.levelAt(0, 0))
        assertEquals(AvailabilityLevel.LOW, state.levelAt(0, 1))
        assertEquals(AvailabilityLevel.MEDIUM, state.levelAt(1, 0))
        assertEquals(AvailabilityLevel.HIGH, state.levelAt(1, 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `row size must match date count`() {
        heatmapState(listOf(listOf(1)))
    }

    private fun heatmapState(counts: List<List<Int>>) = AvailabilityHeatmapUiState(
        monthLabel = "2026년 9월",
        dates = listOf(
            AvailabilityDateColumnUiState("date-4", "4일 (금)"),
            AvailabilityDateColumnUiState("date-5", "5일 (토)")
        ),
        rows = counts.mapIndexed { index, row ->
            AvailabilityTimeRowUiState("1$index:00", row)
        }
    )
}