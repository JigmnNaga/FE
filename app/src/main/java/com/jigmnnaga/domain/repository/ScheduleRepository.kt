package com.jigmnnaga.domain.repository

import com.jigmnnaga.domain.model.MySchedule
import com.jigmnnaga.domain.model.ScheduleResult
import com.jigmnnaga.domain.model.ScheduleStatus
import com.jigmnnaga.domain.model.ScheduleSubmission

interface ScheduleRepository {
    /** 내가 제출한 일정을 조회한다. 수정 화면의 초기값으로 쓴다. */
    suspend fun getMySchedule(appointmentId: Long): ScheduleResult<MySchedule>

    /** 참여자 전체의 일정 응답 현황과 장소별 선택 현황을 조회한다. */
    suspend fun getScheduleStatus(appointmentId: Long): ScheduleResult<ScheduleStatus>

    /** 일정을 제출하거나 수정한다. 기존 선택은 전부 새 값으로 바뀐다. */
    suspend fun submitSchedule(appointmentId: Long, submission: ScheduleSubmission): ScheduleResult<Unit>
}
