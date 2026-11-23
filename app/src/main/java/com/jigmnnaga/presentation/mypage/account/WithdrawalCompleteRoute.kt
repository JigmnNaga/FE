package com.jigmnnaga.presentation.mypage.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable

/** 회원탈퇴 완료 화면 네비게이션 목적지 타입. */
@Serializable
data object WithdrawalCompleteRoute

@Composable
fun WithdrawalCompleteRoute(
    onLeaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AccountActionCompleteScreen(
        actionType = AccountActionType.WITHDRAWAL,
        onLeaveClick = onLeaveClick,
        modifier = modifier
    )
}
