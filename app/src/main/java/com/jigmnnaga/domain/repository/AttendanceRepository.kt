package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.AttendanceAnswer
import com.jigmnnaga.domain.model.AttendanceResult

interface AttendanceRepository {
    /** 확정된 약속에 참석 여부를 응답한다. 약속 시간이 지나기 전까지 다시 보내서 바꿀 수 있다. */
    suspend fun respond(appointmentId: Long, answer: AttendanceAnswer): AttendanceResult<Unit>
}
