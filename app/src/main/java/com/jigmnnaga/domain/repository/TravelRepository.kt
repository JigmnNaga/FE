package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.TravelMode
import com.jigmnnaga.domain.model.TravelOptions
import com.jigmnnaga.domain.model.TravelResult

interface TravelRepository {
    /** 내 출발 위치로 쓸 이름과 좌표를 약속에 등록한다. */
    suspend fun registerDepartureLocation(
        appointmentId: Long,
        name: String,
        latitude: Double,
        longitude: Double
    ): TravelResult<Unit>

    /** 주어진 위치에서 약속 장소까지 가는 이동수단별 대표 경로를 조회한다. */
    suspend fun getTravelOptions(
        appointmentId: Long,
        latitude: Double,
        longitude: Double
    ): TravelResult<TravelOptions>

    /** 가려는 이동수단과 그 예상 소요 시간(분)을 저장한다. */
    suspend fun selectTravelMode(
        appointmentId: Long,
        mode: TravelMode,
        durationMinutes: Int
    ): TravelResult<Unit>

    /** 출발 상태로 바꾼다. */
    suspend fun depart(appointmentId: Long): TravelResult<Unit>

    /** 도착 상태로 바꾼다. 출발한 뒤에만 할 수 있다. */
    suspend fun arrive(appointmentId: Long): TravelResult<Unit>
}
