package com.jigmnnaga.presentation.appointment

import com.jigmnnaga.domain.model.AppointmentDetail
import com.jigmnnaga.domain.model.AppointmentMemberRole

/** 내 역할이 방장일 때만 방장 약속방을 보여 준다. 역할을 모르거나 방장이 아니면 참여자 약속방이다. */
internal fun AppointmentDetail.opensHostRoom(): Boolean = myRole == AppointmentMemberRole.HOST

/** 홈 목록처럼 약속 ID가 문자열로 오는 곳에서 약속방 목적지를 만든다. 숫자가 아니면 null이다. */
internal fun String.toAppointmentRoomDestination(): AppointmentRoomDestination? =
    trim().toLongOrNull()?.let(::AppointmentRoomDestination)
