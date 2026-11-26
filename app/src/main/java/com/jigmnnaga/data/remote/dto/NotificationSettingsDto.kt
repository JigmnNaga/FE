package com.jigmnnaga.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * 전역 알림 설정. 조회 응답과 저장 요청이 같은 모양이다.
 * 필드가 BE [NotificationSettingsResponse] / [NotificationSettingsRequest]와 1:1 대응한다.
 */
@Serializable
data class NotificationSettingsDto(
    val allNotificationsEnabled: Boolean,
    val appointmentReminderEnabled: Boolean,
    val departureTimeEnabled: Boolean,
    val friendArrivalEnabled: Boolean,
    val marketingEnabled: Boolean,
)
