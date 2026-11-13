package com.jigmnnaga.presentation.common.login

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable

/** 네비게이션 목적지 타입. */
@Serializable
data object LoginRoute

/**
 * 로그인 화면의 Route Composable.
 * [LoginViewModel]과 [KakaoLoginClient]를 연결하고,
 * 플랫폼 의존 코드(Activity context, Snackbar)를 처리합니다.
 *
 * [onLoginSuccess]는 백엔드 로그인 API 연동 후 홈 화면 이동에 연결합니다.
 */
@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current as Activity
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is LoginUiState.Success -> {
                onLoginSuccess()
                // Success 상태를 즉시 소비하여 Activity 재생성·화면 재진입 시
                // onLoginSuccess()가 중복 호출되지 않도록 합니다.
                viewModel.clearSuccess()
            }
            is LoginUiState.Failure -> {
                val message = when (state) {
                    LoginUiState.Failure.SdkNotInitialized ->
                        "카카오 로그인 설정이 완료되지 않았습니다."
                    LoginUiState.Failure.LoginError ->
                        "로그인에 실패했습니다. 다시 시도해 주세요."
                }
                snackbarHostState.showSnackbar(message)
                viewModel.clearFailure()
            }
            else -> Unit
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LoginScreen(
            onKakaoLoginClick = { viewModel.login(activity) },
            onTermsClick = onTermsClick,
            onPrivacyClick = onPrivacyClick,
            isLoading = uiState is LoginUiState.Loading
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}
