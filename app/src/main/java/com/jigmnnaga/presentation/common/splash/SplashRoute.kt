package com.jigmnnaga.presentation.common.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jigmnnaga.di.TokenEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute

/**
 * 스플래시 화면의 Route Composable.
 *
 * 앱 실행 시 토큰 유효성을 확인하고 결과에 따라 화면을 이동합니다.
 * 확인 중에는 스플래시 화면을 유지하며, 완료되면 [onAuthenticated] 또는 [onUnauthenticated]를 호출합니다.
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
                    val entryPoint = EntryPointAccessors.fromApplication(
                        applicationContext,
                        TokenEntryPoint::class.java
                    )
                    SplashViewModel(entryPoint.tokenManager())
                }
            }
        }
    )
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            SplashAuthState.Authenticated -> onAuthenticated()
            SplashAuthState.Unauthenticated -> onUnauthenticated()
            SplashAuthState.Loading -> Unit
        }
    }

    SplashScreen(modifier = modifier)
}
