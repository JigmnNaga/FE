package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.AppointmentCreateResult
import com.jigmnnaga.domain.model.CreatedAppointment
import com.jigmnnaga.domain.model.NewAppointment

interface AppointmentCreateRepository {
    /**
     * 약속을 만들고 초대 링크를 받는다. 같은 요청을 다시 보내면 약속이 하나 더 만들어지므로,
     * 성공한 뒤에는 다시 호출하지 않는다.
     */
    suspend fun create(appointment: NewAppointment): AppointmentCreateResult<CreatedAppointment>
}
