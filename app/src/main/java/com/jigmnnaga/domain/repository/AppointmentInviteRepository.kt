package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.AppointmentInvite
import com.jigmnnaga.domain.model.InviteOperationResult

interface AppointmentInviteRepository {
    suspend fun getInvite(inviteCode: String): InviteOperationResult<AppointmentInvite>
    suspend fun acceptInvite(inviteCode: String): InviteOperationResult<Long>
    suspend fun declineInvite(inviteCode: String): InviteOperationResult<Unit>
}
