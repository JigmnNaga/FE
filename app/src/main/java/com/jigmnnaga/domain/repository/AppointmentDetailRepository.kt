package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.AppointmentDetail
import com.jigmnnaga.domain.model.AppointmentDetailResult

interface AppointmentDetailRepository {
    suspend fun getAppointmentDetail(appointmentId: Long): AppointmentDetailResult<AppointmentDetail>
}
