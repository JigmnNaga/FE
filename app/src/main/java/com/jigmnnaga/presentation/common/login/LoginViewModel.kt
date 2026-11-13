package com.jigmnnaga.presentation.common.login

import android.app.Activity
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 로그인 화면의 상태를 관리합니다.
 *
 * 카카오 SDK 호출은 [KakaoLoginClient]에 위임하고,
 * Activity는 호출 시점에 파라미터로 전달받아 저장하지 않습니다.
 *
 * @param kakaoLoginClient 테스트 시 목(Mock)으로 교체할 수 있습니다.
 */
class LoginViewModel(
    private val kakaoLoginClient: KakaoLoginClient = KakaoLoginClientImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** 카카오 로그인을 시작합니다. 이미 진행 중이면 무시합니다. */
    fun login(activity: Activity) {
        if (_uiState.value is LoginUiState.Loading) return
        _uiState.value = LoginUiState.Loading
        // Kakao SDK 콜백은 메인 스레드에서 전달됩니다.
        kakaoLoginClient.login(activity) { result ->
            _uiState.value = when (result) {
                KakaoLoginResult.Success -> LoginUiState.Success
                KakaoLoginResult.Cancelled -> LoginUiState.Idle
                KakaoLoginResult.SdkNotInitialized -> LoginUiState.Failure.SdkNotInitialized
                is KakaoLoginResult.Failure -> LoginUiState.Failure.LoginError
            }
        }
    }

    /**
     * 성공 상태를 Idle로 초기화합니다.
     * [LoginRoute]에서 [LoginUiState.Success] 처리 직후 호출하여
     * Activity 재생성·화면 재진입 시 중복 처리를 방지합니다.
     */
    fun clearSuccess() {
        if (_uiState.value is LoginUiState.Success) {
            _uiState.value = LoginUiState.Idle
        }
    }

    /** 실패 상태를 Idle로 초기화합니다. 에러 메시지 표시 후 호출합니다. */
    fun clearFailure() {
        if (_uiState.value is LoginUiState.Failure) {
            _uiState.value = LoginUiState.Idle
        }
    }
}
