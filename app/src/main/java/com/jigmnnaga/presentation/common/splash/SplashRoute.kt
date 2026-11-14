package com.jigmnnaga.presentation.common.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jigmnnaga.core.designsystem.theme.AppColors
import com.jigmnnaga.di.AuthEntryPoint
import com.jigmnnaga.di.TokenEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute

/**
 * 스플래시 화면의 Route Composable.
 *
 * 앱 실행 시 토큰 유효성을 확인하고 결과에 따라 화면을 이동합니다.
 * - Loading: 스플래시 화면 유지
 * - Authenticated → [onAuthenticated]
 * - Unauthenticated → [onUnauthenticated]
 * - RetryableError: 오류 안내 + 재시도 버튼 표시
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

    LaunchedEffect(authState) {
        when (authState) {
            SplashAuthState.Authenticated -> onAuthenticated()
            SplashAuthState.Unauthenticated -> onUnauthenticated()
            SplashAuthState.Loading, SplashAuthState.RetryableError -> Unit
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        SplashScreen()

        if (authState is SplashAuthState.RetryableError) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "네트워크 연결을 확인해 주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.MainGreen.Green700
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = viewModel::retry,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.MainGreen.Green700
                    )
                ) {
                    Text("다시 시도")
                }
            }
        }
    }
}
