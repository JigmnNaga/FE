package com.jigmnnaga.domain.model

/** 알림 설정을 서버와 맞춘 결과. */
sealed interface NotificationSettingsSyncResult {
    data object Success : NotificationSettingsSyncResult

    /** 로그인이 필요하다(토큰이 없거나 만료됐다). */
    data object LoginRequired : NotificationSettingsSyncResult

    /** 서버와 맞추지 못했다. [retryable]이 true면 연결 문제나 서버 오류라 다시 시도할 수 있다. */
    data class Failure(val retryable: Boolean) : NotificationSettingsSyncResult
}
