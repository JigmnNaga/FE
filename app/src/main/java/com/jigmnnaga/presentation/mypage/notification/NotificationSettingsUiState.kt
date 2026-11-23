package com.jigmnnaga.presentation.mypage.notification

/**
 * MYP-003 알림 설정 화면 UI 상태.
 * API 연동 전까지 기본값(전체·약속 켜짐, 마케팅 꺼짐)으로 초기화한다.
 */
data class NotificationSettingsUiState(
    val allNotificationsEnabled: Boolean = true,
    val appointmentReminderEnabled: Boolean = true,
    val departureTimeEnabled: Boolean = true,
    val friendArrivalEnabled: Boolean = true,
    val marketingEnabled: Boolean = false,
)
