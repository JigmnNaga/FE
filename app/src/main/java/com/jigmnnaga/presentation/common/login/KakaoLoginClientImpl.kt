package com.jigmnnaga.presentation.common.login

import android.app.Activity
import com.jigmnnaga.BuildConfig
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient

/**
 * 카카오 SDK를 사용하는 [KakaoLoginClient] 구현체.
 *
 * - 카카오톡이 설치된 경우 카카오톡 로그인 우선 시도
 * - 카카오톡 로그인 실패 시 카카오계정 로그인으로 자동 전환
 * - 사용자 취소와 오류를 명확하게 구분
 * - 액세스 토큰 값은 외부에 노출하지 않음
 */
class KakaoLoginClientImpl : KakaoLoginClient {

    override fun login(activity: Activity, onResult: (KakaoLoginResult) -> Unit) {
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) {
            onResult(KakaoLoginResult.SdkNotInitialized)
            return
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(activity)) {
            loginWithKakaoTalk(activity, onResult)
        } else {
            loginWithKakaoAccount(activity, onResult)
        }
    }

    private fun loginWithKakaoTalk(activity: Activity, onResult: (KakaoLoginResult) -> Unit) {
        UserApiClient.instance.loginWithKakaoTalk(activity) { token, error ->
            when {
                error != null -> {
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        // 사용자가 카카오톡 로그인 화면을 직접 닫은 경우
                        onResult(KakaoLoginResult.Cancelled)
                    } else {
                        // 카카오톡 앱 로그인 실패 → 카카오계정으로 재시도
                        loginWithKakaoAccount(activity, onResult)
                    }
                }
                token != null -> {
                    android.util.Log.d("KakaoLogin", "카카오톡 로그인 성공 (토큰 앞 10자: ${token.accessToken.take(10)}...)")
                    onResult(KakaoLoginResult.Success)
                }
                else -> onResult(KakaoLoginResult.Failure(Exception("카카오톡 로그인 응답 없음")))
            }
        }
    }

    private fun loginWithKakaoAccount(activity: Activity, onResult: (KakaoLoginResult) -> Unit) {
        UserApiClient.instance.loginWithKakaoAccount(activity) { token, error ->
            when {
                error != null -> {
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        onResult(KakaoLoginResult.Cancelled)
                    } else {
                        android.util.Log.e("KakaoLogin", "카카오계정 로그인 실패", error)
                        onResult(KakaoLoginResult.Failure(error))
                    }
                }
                token != null -> {
                    android.util.Log.d("KakaoLogin", "카카오계정 로그인 성공 (토큰 앞 10자: ${token.accessToken.take(10)}...)")
                    onResult(KakaoLoginResult.Success)
                }
                else -> onResult(KakaoLoginResult.Failure(Exception("카카오계정 로그인 응답 없음")))
            }
        }
    }
}
