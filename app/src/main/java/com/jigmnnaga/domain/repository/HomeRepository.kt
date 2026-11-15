package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.HomeAppointmentListResult
import com.jigmnnaga.domain.model.NextAppointmentResult
import com.jigmnnaga.domain.model.PendingInviteCountResult

interface HomeRepository {
    suspend fun getUpcomingAppointments(): HomeAppointmentListResult
    suspend fun getPastAppointments(): HomeAppointmentListResult
    suspend fun getNextAppointment(): NextAppointmentResult
    suspend fun getPendingInviteCount(): PendingInviteCountResult
}
