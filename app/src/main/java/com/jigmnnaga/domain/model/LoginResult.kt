package com.jigmnnaga.domain.model

sealed interface LoginResult {
    /** 로그인 성공. [isNewMember]가 true이면 신규 회원. */
    data class Success(val isNewMember: Boolean) : LoginResult

    /** 카카오 액세스 토큰이 유효하지 않음 (KAKAO_TOKEN_INVALID). */
    data object InvalidKakaoToken : LoginResult

    /** 탈퇴한 회원 (WITHDRAWN_MEMBER). */
    data object WithdrawnMember : LoginResult

    /** 카카오 닉네임 동의 항목 누락 (KAKAO_ACCOUNT_INFO_MISSING). */
    data object KakaoAccountInfoMissing : LoginResult

    /** 네트워크 연결 오류. [debugInfo]는 개발 진단용 (예외 클래스명). */
    data class NetworkError(val debugInfo: String? = null) : LoginResult

    /** 서버 오류 또는 그 외 예상치 못한 오류. [debugInfo]는 개발 진단용. */
    data class ServerError(val debugInfo: String? = null) : LoginResult
}
