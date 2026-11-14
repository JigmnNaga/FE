package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.AppointmentManageResult
import com.jigmnnaga.domain.model.CandidateLocationChange
import com.jigmnnaga.domain.model.NewAppointmentDateType
import com.jigmnnaga.domain.model.ReminderSendSummary

/** 방장이 약속방을 관리하는 API 모음. 방장이 아니면 [AppointmentManageResult.NotHost]를 돌려준다. */
interface AppointmentManageRepository {
    suspend fun updateTitle(appointmentId: Long, title: String): AppointmentManageResult<Unit>

    /** [dates]는 `yyyy-MM-dd` 문자열이다. 선택 방식에 맞게 펼친 날짜 목록을 넘긴다. */
    suspend fun updateCandidateDates(
        appointmentId: Long,
        dateType: NewAppointmentDateType,
        dates: List<String>
    ): AppointmentManageResult<Unit>

    suspend fun updateCandidateLocations(
        appointmentId: Long,
        change: CandidateLocationChange
    ): AppointmentManageResult<Unit>

    /** [deadline]은 `yyyy-MM-ddTHH:mm:ss` 형식이다. */
    suspend fun updateResponseDeadline(appointmentId: Long, deadline: String): AppointmentManageResult<Unit>

    suspend fun cancel(appointmentId: Long): AppointmentManageResult<Unit>
    suspend fun delete(appointmentId: Long): AppointmentManageResult<Unit>
    suspend fun delegateHost(appointmentId: Long, newHostMemberId: Long): AppointmentManageResult<Unit>
    suspend fun kickParticipant(appointmentId: Long, targetMemberId: Long): AppointmentManageResult<Unit>

    /** 약속방을 나간다. 방장이 나갈 때는 [newHostMemberId]로 다음 방장을 넘겨야 한다. */
    suspend fun leave(appointmentId: Long, newHostMemberId: Long? = null): AppointmentManageResult<Unit>

    /** 이 약속방의 푸시 알림을 켜거나 끈다. 알림함에는 그대로 쌓인다. */
    suspend fun updateNotificationSetting(appointmentId: Long, enabled: Boolean): AppointmentManageResult<Unit>

    suspend fun sendReminders(appointmentId: Long): AppointmentManageResult<ReminderSendSummary>
}
