package com.jigmnnaga.presentation.mypage.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable

/** 로그아웃 완료 화면 네비게이션 목적지 타입. */
@Serializable
data object LogoutCompleteRoute

@Composable
fun LogoutCompleteRoute(
    onLeaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AccountActionCompleteScreen(
        actionType = AccountActionType.LOGOUT,
        onLeaveClick = onLeaveClick,
        modifier = modifier
    )
}
