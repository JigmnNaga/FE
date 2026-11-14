package com.jigmnnaga.presentation.common.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.core.datastore.TokenManager
import com.jigmnnaga.domain.model.ReissueResult
import com.jigmnnaga.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SplashAuthState {
    data object Loading : SplashAuthState
    data object Authenticated : SplashAuthState
    data object Unauthenticated : SplashAuthState

    /**
     * 네트워크 오류 또는 서버 일시적 오류.
     * 저장된 토큰을 삭제하지 않으며 [SplashViewModel.retry]로 재시도할 수 있습니다.
     */
    data object RetryableError : SplashAuthState
}

/**
 * 앱 실행 시 저장된 토큰의 유효성을 확인하여 인증 상태를 결정합니다.
 *
 * - Case A: Access Token 유효 → [SplashAuthState.Authenticated]
 * - Case B: Access Token 만료 + Refresh 성공 → [SplashAuthState.Authenticated]
 * - Case C: 토큰 없음 → [SplashAuthState.Unauthenticated]
 * - Case D: Refresh Token 만료/무효 → 토큰 삭제 → [SplashAuthState.Unauthenticated]
 * - Case E: 네트워크/서버 오류 → 토큰 유지 → [SplashAuthState.RetryableError]
 * - Case F: 복호화 실패 → 토큰 정리 → [SplashAuthState.Unauthenticated]
 */
class SplashViewModel(
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<SplashAuthState>(SplashAuthState.Loading)
    val authState: StateFlow<SplashAuthState> = _authState.asStateFlow()

    init {
        checkAuthState()
    }

    /** 네트워크 오류 등 재시도 가능한 상태에서 인증 확인을 다시 시도합니다. */
    fun retry() {
        if (_authState.value is SplashAuthState.RetryableError) {
            _authState.value = SplashAuthState.Loading
            checkAuthState()
        }
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            _authState.value = try {
                // Case C, F: 토큰 없음 또는 복호화 실패
                val isValid = tokenManager.validateAndClearInvalidTokens()
                if (!isValid) {
                    SplashAuthState.Unauthenticated
                } else if (!tokenManager.isAccessTokenExpired()) {
                    // Case A: Access Token 유효
                    SplashAuthState.Authenticated
                } else {
                    // Access Token 만료 → Refresh Token 재발급 시도
                    when (val result = authRepository.reissueTokens()) {
                        ReissueResult.Success ->
                            SplashAuthState.Authenticated             // Case B
                        ReissueResult.TokenInvalidOrExpired -> {
                            tokenManager.clearTokens()
                            SplashAuthState.Unauthenticated           // Case D
                        }
                        is ReissueResult.NetworkError ->
                            SplashAuthState.RetryableError            // Case E
                        is ReissueResult.ServerError ->
                            SplashAuthState.RetryableError            // Case E
                    }
                }
            } catch (e: Exception) {
                SplashAuthState.Unauthenticated
            }
        }
    }
}
