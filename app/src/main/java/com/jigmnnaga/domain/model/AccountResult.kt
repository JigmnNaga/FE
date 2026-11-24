package com.jigmnnaga.domain.model

sealed interface LogoutResult {
    data object Success : LogoutResult

    /** 액세스 토큰 없음 — 이미 로그아웃 상태. */
    data object LoginRequired : LogoutResult

    /** 네트워크 또는 서버 오류. [retryable] 이 true이면 재시도 가능. */
    data class Failure(val retryable: Boolean) : LogoutResult
}

sealed interface WithdrawResult {
    data object Success : WithdrawResult

    /** 액세스 토큰 없음 — 이미 로그아웃 상태. */
    data object LoginRequired : WithdrawResult

    /** 네트워크 또는 서버 오류. [retryable] 이 true이면 재시도 가능. */
    data class Failure(val retryable: Boolean) : WithdrawResult
}
