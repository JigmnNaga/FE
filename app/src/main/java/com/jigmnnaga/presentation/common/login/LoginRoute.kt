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
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jigmnnaga.BuildConfig
import com.jigmnnaga.di.AuthEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

/** 네비게이션 목적지 타입. */
@Serializable
data object LoginRoute

/**
 * 로그인 화면의 Route Composable.
 * [LoginViewModel]과 [KakaoLoginClient], [com.jigmnnaga.domain.repository.AuthRepository]를 연결하고,
 * 플랫폼 의존 코드(Activity context, Snackbar)를 처리합니다.
 */
@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val applicationContext = LocalContext.current.applicationContext
    val viewModel: LoginViewModel = viewModel(
        factory = remember(applicationContext) {
            viewModelFactory {
                initializer {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        applicationContext,
                        AuthEntryPoint::class.java
                    )
                    LoginViewModel(
                        kakaoLoginClient = KakaoLoginClientImpl(),
                        authRepository = entryPoint.authRepository()
                    )
                }
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current as Activity
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is LoginUiState.Success -> {
                delay(1_000)
                onLoginSuccess()
                // Success 상태를 소비하여 Activity 재생성·화면 재진입 시
                // onLoginSuccess()가 중복 호출되지 않도록 합니다.
                viewModel.clearSuccess()
            }
            is LoginUiState.Failure -> {
                val message = when (state) {
                    LoginUiState.Failure.SdkNotInitialized ->
                        "카카오 로그인 설정이 완료되지 않았습니다."
                    LoginUiState.Failure.KakaoAuthFailed ->
                        "로그인에 실패했습니다. 다시 시도해 주세요."
                    LoginUiState.Failure.InvalidKakaoToken ->
                        "유효하지 않은 카카오 토큰입니다. 다시 로그인해 주세요."
                    LoginUiState.Failure.WithdrawnMember ->
                        "탈퇴한 회원입니다."
                    LoginUiState.Failure.KakaoAccountInfoMissing ->
                        "카카오 동의 항목(닉네임)이 누락되어 로그인할 수 없습니다."
                    is LoginUiState.Failure.NetworkError ->
                        if (BuildConfig.DEBUG && state.debugInfo != null)
                            "네트워크 연결을 확인해 주세요. (${state.debugInfo})"
                        else
                            "네트워크 연결을 확인해 주세요."
                    is LoginUiState.Failure.ServerError ->
                        if (BuildConfig.DEBUG && state.debugInfo != null)
                            "서버 오류가 발생했습니다. (${state.debugInfo})"
                        else
                            "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
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
            isLoading = uiState is LoginUiState.Loading,
            isSuccess = uiState is LoginUiState.Success
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}
