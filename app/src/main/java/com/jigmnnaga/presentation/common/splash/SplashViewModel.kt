package com.jigmnnaga.presentation.common.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jigmnnaga.core.datastore.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SplashAuthState {
    data object Loading : SplashAuthState
    data object Authenticated : SplashAuthState
    data object Unauthenticated : SplashAuthState
}

/**
 * 앱 실행 시 저장된 토큰의 유효성을 확인하여 인증 상태를 결정합니다.
 *
 * - 유효한 Access Token이 있으면 [SplashAuthState.Authenticated]
 * - 토큰이 없거나 만료·손상된 경우 [SplashAuthState.Unauthenticated]
 * - 토큰 재발급 API가 없으므로 Access Token 만료 시 로그인 화면으로 이동합니다.
 * - DataStore 오류 등 예외가 발생하면 안전하게 로그인 화면으로 이동합니다.
 */
class SplashViewModel(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _authState = MutableStateFlow<SplashAuthState>(SplashAuthState.Loading)
    val authState: StateFlow<SplashAuthState> = _authState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            _authState.value = try {
                val isValid = tokenManager.validateAndClearInvalidTokens()
                if (!isValid) {
                    SplashAuthState.Unauthenticated
                } else {
                    val isExpired = tokenManager.isAccessTokenExpired()
                    if (isExpired) {
                        // 토큰 재발급 API 미구현 상태이므로 로그인 화면으로 이동합니다.
                        SplashAuthState.Unauthenticated
                    } else {
                        SplashAuthState.Authenticated
                    }
                }
            } catch (e: Exception) {
                SplashAuthState.Unauthenticated
            }
        }
    }
}
