package com.jigmnnaga.presentation.mypage.notification

/**
 * MYP-003 알림 설정 화면 UI 상태.
 * 기본값(전체·약속 켜짐, 마케팅 꺼짐)은 DataStore에 저장된 값이 없을 때 사용된다.
 */
data class NotificationSettingsUiState(
    val allNotificationsEnabled: Boolean = true,
    val appointmentReminderEnabled: Boolean = true,
    val departureTimeEnabled: Boolean = true,
    val friendArrivalEnabled: Boolean = true,
    val marketingEnabled: Boolean = false,
    /** 설정 저장에 실패했을 때 true. [NotificationSettingsViewModel.consumeSaveFailed]로 소비한다. */
    val saveFailed: Boolean = false,
    /** 로그인이 만료됐을 때 true. [NotificationSettingsViewModel.consumeLoginRequired]로 소비한다. */
    val loginRequired: Boolean = false,
)
