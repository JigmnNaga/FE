package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.ConfirmDetailPlace
import com.jigmnnaga.domain.model.ConfirmationResult
import com.jigmnnaga.domain.model.TimeCandidates

interface ConfirmationRepository {
    /** 가능 인원이 많은 시간대 후보를 조회한다. 방장 확정 화면의 날짜·시간 후보로 쓴다. */
    suspend fun getTimeCandidates(appointmentId: Long): ConfirmationResult<TimeCandidates>

    /** 후보 날짜 중 하나와 시간을 약속 일정으로 확정한다. 다시 호출하면 변경된다. */
    suspend fun confirmDateTime(
        appointmentId: Long,
        date: String,
        time: String
    ): ConfirmationResult<Unit>

    /**
     * 후보 장소 중 하나를 확정한다. [detailPlace]가 없으면 세부 위치는 나중에 정하며,
     * 그동안은 확정 전 상태다. 다시 호출하면 변경된다.
     */
    suspend fun confirmLocation(
        appointmentId: Long,
        locationId: Long,
        detailPlace: ConfirmDetailPlace?
    ): ConfirmationResult<Unit>
}
