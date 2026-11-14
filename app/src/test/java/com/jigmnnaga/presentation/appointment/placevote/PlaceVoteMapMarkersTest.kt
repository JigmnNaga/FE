package com.jigmnnaga.presentation.appointment.placevote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceVoteMapMarkersTest {

    @Test
    fun `좌표가 있는 후보만 마커로 바뀐다`() {
        val candidates = listOf(
            PlaceCandidateUiModel(id = "a", name = "강남역", latitude = 37.4979, longitude = 127.0276),
            PlaceCandidateUiModel(id = "b", name = "좌표 없음"),
            PlaceCandidateUiModel(id = "c", name = "위도만 있음", latitude = 37.5),
            PlaceCandidateUiModel(id = "d", name = "경도만 있음", longitude = 127.0)
        )

        val markers = candidates.toMapMarkers()

        assertEquals(1, markers.size)
        assertEquals("a", markers.single().id)
        assertEquals(37.4979, markers.single().latitude, 0.0)
        assertEquals(127.0276, markers.single().longitude, 0.0)
    }

    @Test
    fun `후보가 없으면 마커도 없다`() {
        assertTrue(emptyList<PlaceCandidateUiModel>().toMapMarkers().isEmpty())
    }

    @Test
    fun `마커 순서는 후보 순서를 따른다`() {
        val candidates = listOf(
            PlaceCandidateUiModel(id = "1", name = "첫째", latitude = 37.1, longitude = 127.1),
            PlaceCandidateUiModel(id = "2", name = "둘째", latitude = 37.2, longitude = 127.2)
        )

        assertEquals(listOf("1", "2"), candidates.toMapMarkers().map { it.id })
    }
}
