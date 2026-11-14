package com.jigmnnaga.presentation.common.login

sealed interface LoginUiState {
    /** 초기 상태 또는 취소 후 복귀 상태. */
    data object Idle : LoginUiState

    /** 카카오 로그인 및 백엔드 API 호출 진행 중. */
    data object Loading : LoginUiState

    /** 로그인 성공 — JWT 저장 완료. */
    data object Success : LoginUiState

    sealed interface Failure : LoginUiState {
        /** 카카오 네이티브 앱 키 미설정으로 SDK 초기화 실패. */
        data object SdkNotInitialized : Failure

        /** 카카오 SDK 수준 로그인 실패 (네트워크, 서버 등). */
        data object KakaoAuthFailed : Failure

        /** 카카오 액세스 토큰이 유효하지 않음 (KAKAO_TOKEN_INVALID). */
        data object InvalidKakaoToken : Failure

        /** 탈퇴한 회원 (WITHDRAWN_MEMBER). */
        data object WithdrawnMember : Failure

        /** 카카오 닉네임 동의 항목 누락 (KAKAO_ACCOUNT_INFO_MISSING). */
        data object KakaoAccountInfoMissing : Failure

        /**
         * 네트워크 연결 오류.
         * [debugInfo]는 개발 진단용 (예외 클래스명) — devDebug 빌드에서만 화면에 노출합니다.
         */
        data class NetworkError(val debugInfo: String? = null) : Failure

        /**
         * 서버 오류 또는 그 외 예상치 못한 오류.
         * [debugInfo]는 개발 진단용 (예외 클래스명 또는 HTTP 상태코드) — devDebug 빌드에서만 화면에 노출합니다.
         */
        data class ServerError(val debugInfo: String? = null) : Failure
    }
}
