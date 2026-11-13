package com.jigmnnaga.presentation.common.login

import android.app.Activity

/** 카카오 로그인 SDK 호출을 추상화한 인터페이스. */
interface KakaoLoginClient {

    /**
     * 카카오톡이 설치된 경우 카카오톡 로그인을 우선 시도하고,
     * 불가능한 경우 카카오계정 로그인으로 전환합니다.
     * 결과는 메인 스레드에서 [onResult]로 전달됩니다.
     */
    fun login(activity: Activity, onResult: (KakaoLoginResult) -> Unit)
}

/** [KakaoLoginClient.login] 호출 결과. */
sealed interface KakaoLoginResult {
    /** 로그인 성공 — 액세스 토큰 획득 완료. */
    data object Success : KakaoLoginResult

    /** 사용자가 직접 로그인을 취소한 경우. */
    data object Cancelled : KakaoLoginResult

    /** 카카오 네이티브 앱 키가 설정되지 않아 SDK가 초기화되지 않은 경우. */
    data object SdkNotInitialized : KakaoLoginResult

    /** 네트워크 오류, 서버 오류 등 그 외 로그인 실패. */
    data class Failure(val cause: Throwable) : KakaoLoginResult
}
