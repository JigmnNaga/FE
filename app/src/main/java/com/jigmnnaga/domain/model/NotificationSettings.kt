package com.jigmnnaga.domain.model

/**
 * 알림 설정. 기본값은 전체·약속 알림 켜짐, 마케팅 꺼짐.
 * 서버를 최종 기준으로 사용하고, DataStore는 로컬 캐시로 유지한다.
 */
data class NotificationSettings(
    val allNotificationsEnabled: Boolean = true,
    val appointmentReminderEnabled: Boolean = true,
    val departureTimeEnabled: Boolean = true,
    val friendArrivalEnabled: Boolean = true,
    val marketingEnabled: Boolean = false,
)
