package com.jigmnnaga.domain.model

/**
 * 알림 설정. 기본값은 전체·약속 알림 켜짐, 마케팅 꺼짐.
 * 현재는 기기 DataStore에만 저장하며, BE 전역 알림 설정 API 추가 시 서버와 동기화한다.
 */
data class NotificationSettings(
    val allNotificationsEnabled: Boolean = true,
    val appointmentReminderEnabled: Boolean = true,
    val departureTimeEnabled: Boolean = true,
    val friendArrivalEnabled: Boolean = true,
    val marketingEnabled: Boolean = false,
)

/** 알림 설정 저장 결과. */
sealed interface NotificationSettingsSaveResult {
    data object Success : NotificationSettingsSaveResult

    /** 저장 실패. [retryable]이 true면 IO 오류라 다시 시도할 수 있다. */
    data class Failure(val retryable: Boolean) : NotificationSettingsSaveResult
}
