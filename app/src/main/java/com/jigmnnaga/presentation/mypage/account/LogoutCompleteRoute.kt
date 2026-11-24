package com.jigmnnaga.presentation.mypage.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.Serializable

/** 로그아웃 완료 화면 네비게이션 목적지 타입. */
@Serializable
data object LogoutCompleteRoute

@Composable
fun LogoutCompleteRoute(
    viewModel: AccountViewModel,
    onLeaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 화면 진입 시 로그아웃 API 호출
    LaunchedEffect(Unit) {
        viewModel.logout()
    }

    // 로그아웃 성공 또는 이미 로그아웃 상태 → 로그인 화면으로 이동
    LaunchedEffect(state.logoutSuccess, state.loginRequired) {
        if (state.logoutSuccess || state.loginRequired) {
            onLeaveClick()
        }
    }

    AccountActionCompleteScreen(
        actionType = AccountActionType.LOGOUT,
        onLeaveClick = onLeaveClick,
        modifier = modifier
    )
}
