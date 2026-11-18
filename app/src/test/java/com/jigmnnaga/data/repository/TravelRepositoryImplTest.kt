package com.jigmnnaga.data.repository

import com.jigmnnaga.data.mapper.toDomain
import com.jigmnnaga.data.remote.dto.TravelCoordinateDto
import com.jigmnnaga.data.remote.dto.TravelOptionDto
import com.jigmnnaga.data.remote.dto.TravelOptionsDto
import com.jigmnnaga.data.remote.dto.TravelStepDto
import com.jigmnnaga.domain.model.TravelMode
import com.jigmnnaga.domain.model.TravelRejectReason
import com.jigmnnaga.domain.model.TravelResult
import com.jigmnnaga.domain.model.TravelStepType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TravelRepositoryImplTest {
    @Test
    fun `서버 응답을 이동수단 후보로 바꾼다`() {
        val dto = TravelOptionsDto(
            destinationName = "  ",
            appointmentDateTime = "2026-11-20T17:00:00",
            myTravelMode = "BUS",
            options = listOf(
                TravelOptionDto(
                    mode = "BUS",
                    durationMinutes = 35,
                    distanceMeters = 8000,
                    estimatedArrivalTime = "2026-11-20T16:50:00",
                    steps = listOf(
                        TravelStepDto("WALKING", 5, null),
                        TravelStepDto("BUS", 24, "146, 342"),
                        TravelStepDto("전혀 모르는 값", 6, "")
                    ),
                    path = listOf(TravelCoordinateDto(37.5, 127.0))
                )
            )
        )

        val result = dto.toDomain()

        assertNull(result.destinationName)
        assertEquals(TravelMode.BUS, result.myTravelMode)
        val option = result.options.single()
        assertEquals(35, option.durationMinutes)
        assertEquals(
            listOf(TravelStepType.WALK, TravelStepType.BUS, TravelStepType.OTHER),
            option.steps.map { it.type }
        )
        assertEquals("146, 342", option.steps[1].name)
        assertNull(option.steps[2].name)
        assertEquals(1, option.path.size)
    }

    @Test
    fun `모르는 이동수단은 후보에서 뺀다`() {
        val dto = TravelOptionsDto(
            options = listOf(
                TravelOptionDto(mode = "TRAIN", durationMinutes = 10),
                TravelOptionDto(mode = null, durationMinutes = 10),
                TravelOptionDto(mode = "TAXI", durationMinutes = 22)
            )
        )

        assertEquals(listOf(TravelMode.TAXI), dto.toDomain().options.map { it.mode })
    }

    @Test
    fun `후보가 없는 응답도 빈 목록으로 받는다`() {
        assertEquals(emptyList<Any>(), TravelOptionsDto(options = null).toDomain().options)
    }

    @Test
    fun `서버 오류 코드에 따라 거절 이유를 나눈다`() {
        assertEquals(
            TravelResult.Rejected(TravelRejectReason.DEPARTURE_LOCATION_NOT_REGISTERED),
            mapTravelError(400, "DEPARTURE_LOCATION_NOT_REGISTERED")
        )
        assertEquals(
            TravelResult.Rejected(TravelRejectReason.DESTINATION_NOT_CONFIRMED),
            mapTravelError(400, "TRAVEL_DESTINATION_NOT_CONFIRMED")
        )
        assertEquals(
            TravelResult.Rejected(TravelRejectReason.TIME_PASSED),
            mapTravelError(400, "TRAVEL_MODE_TIME_PASSED")
        )
        assertEquals(
            TravelResult.Rejected(TravelRejectReason.ALREADY_DEPARTED),
            mapTravelError(409, "ALREADY_DEPARTED")
        )
        assertEquals(
            TravelResult.Rejected(TravelRejectReason.ALREADY_ARRIVED),
            mapTravelError(409, "ALREADY_ARRIVED")
        )
        assertEquals(
            TravelResult.Rejected(TravelRejectReason.NOT_YET_DEPARTED),
            mapTravelError(400, "NOT_YET_DEPARTED")
        )
    }

    @Test
    fun `응답 코드에 따라 로그인 필요와 재시도 가능 실패를 나눈다`() {
        assertEquals(TravelResult.LoginRequired, mapTravelError(401, null))
        assertEquals(TravelResult.LoginRequired, mapTravelError(403, "EXPIRED_ACCESS_TOKEN"))
        assertEquals(TravelResult.Failure(retryable = true), mapTravelError(500, null))
        assertEquals(TravelResult.Failure(retryable = false), mapTravelError(418, null))
        assertEquals(
            TravelResult.Rejected(TravelRejectReason.NOT_PARTICIPANT),
            mapTravelError(404, null)
        )
    }
}
