package com.jigmnnaga.presentation.common.login

sealed interface LoginUiState {
    /** 초기 상태 또는 취소 후 복귀 상태. */
    data object Idle : LoginUiState

    /** 카카오 로그인 진행 중. */
    data object Loading : LoginUiState

    /**
     * 카카오 액세스 토큰 획득 성공.
     * 백엔드 로그인 API 연동 후 이 상태에서 화면 이동을 처리합니다.
     */
    data object Success : LoginUiState

    sealed interface Failure : LoginUiState {
        /** 카카오 네이티브 앱 키 미설정으로 SDK 초기화 실패. */
        data object SdkNotInitialized : Failure

        /** 네트워크 오류, 서버 오류 등 로그인 실패. */
        data object LoginError : Failure
    }
}
