package com.jigmnnaga.domain.model

sealed interface ReissueResult {
    /** 재발급 성공 — 새 Access/Refresh Token이 TokenManager에 저장됨. */
    data object Success : ReissueResult

    /**
     * Refresh Token이 만료됐거나 유효하지 않음.
     * (EXPIRED_REFRESH_TOKEN, INVALID_REFRESH_TOKEN)
     * 저장된 토큰을 삭제하고 로그인 화면으로 이동해야 합니다.
     */
    data object TokenInvalidOrExpired : ReissueResult

    /** 네트워크 연결 오류. [debugInfo]는 개발 진단용 (예외 클래스명). */
    data class NetworkError(val debugInfo: String? = null) : ReissueResult

    /** 서버 오류 또는 그 외 예상치 못한 오류. [debugInfo]는 개발 진단용. */
    data class ServerError(val debugInfo: String? = null) : ReissueResult
}
