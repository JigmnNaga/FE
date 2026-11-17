package com.jigmnnaga.presentation.common.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jigmnnaga.di.AuthEntryPoint
import com.jigmnnaga.di.TokenEntryPoint
import com.jigmnnaga.presentation.common.error.ErrorScreen
import com.jigmnnaga.presentation.common.error.ErrorType
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

// 애니메이션이 충분히 보이도록 최소 표시 시간을 보장 (ms)
// 로고(400ms) + 대기(500ms) + 기능 태그(300+240ms) + 여유 ≈ 1500ms
private const val SPLASH_MIN_DURATION_MS = 1500L

@Serializable
data object SplashRoute

/**
 * 스플래시 화면의 Route Composable.
 *
 * 앱 실행 시 토큰 유효성을 확인하고 결과에 따라 화면을 이동합니다.
 * - Loading: 스플래시 화면 유지
 * - Authenticated → [onAuthenticated]
 * - Unauthenticated → [onUnauthenticated]
 * - RetryableError: 공통 오류 화면 표시 (네트워크/서비스 오류 구분)
 */
@Composable
fun SplashRoute(
    onAuthenticated: () -> Unit,
    onUnauthenticated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val applicationContext = LocalContext.current.applicationContext
    val viewModel: SplashViewModel = viewModel(
        factory = remember(applicationContext) {
            viewModelFactory {
                initializer {
                    val tokenEntryPoint = EntryPointAccessors.fromApplication(
                        applicationContext,
                        TokenEntryPoint::class.java
                    )
                    val authEntryPoint = EntryPointAccessors.fromApplication(
                        applicationContext,
                        AuthEntryPoint::class.java
                    )
                    SplashViewModel(
                        tokenManager = tokenEntryPoint.tokenManager(),
                        authRepository = authEntryPoint.authRepository()
                    )
                }
            }
        }
    )
    val authState by viewModel.authState.collectAsState()

    // 오류 상태 → 공통 오류 화면 전체 표시 (스플래시 대신)
    val retryableError = authState as? SplashAuthState.RetryableError
    if (retryableError != null) {
        ErrorScreen(
            type = if (retryableError.isNetworkError) ErrorType.Network else ErrorType.Service,
            onActionClick = viewModel::retry,
            modifier = modifier
        )
        return
    }

    // 최소 표시 시간을 보장: 애니메이션이 모두 끝난 뒤 화면 이동
    var splashReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(SPLASH_MIN_DURATION_MS)
        splashReady = true
    }

    // splashReady 와 authState 가 모두 준비됐을 때만 화면 이동
    LaunchedEffect(authState, splashReady) {
        if (!splashReady) return@LaunchedEffect
        when (authState) {
            SplashAuthState.Authenticated -> onAuthenticated()
            SplashAuthState.Unauthenticated -> onUnauthenticated()
            SplashAuthState.Loading -> Unit
            is SplashAuthState.RetryableError -> Unit  // 위 early-return에서 처리됨
        }
    }

    SplashScreen(modifier = modifier)
}
