package com.jigmnnaga.presentation.mypage.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable

/** 방장 회원탈퇴 안내 화면 네비게이션 목적지 타입. */
@Serializable
data object WithdrawalGuideRoute

/**
 * 방장 회원탈퇴 안내 화면 Route Composable.
 * ViewModel 연동 전까지 Mock 데이터를 사용한다.
 */
@Composable
fun WithdrawalGuideRoute(
    onBackClick: () -> Unit,
    onWithdrawComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showWithdrawalDialog by remember { mutableStateOf(false) }

    WithdrawalGuideScreen(
        rooms = listOf(
            AppointmentRoomInfo("중간고사 카공 모임", 3, true, AppointmentRoomStatus.LEADER_TRANSFER),
            AppointmentRoomInfo("팀플 점심 약속", 1, true, AppointmentRoomStatus.LEADER_DELETE),
            AppointmentRoomInfo("주말 영화", 2, false, AppointmentRoomStatus.MEMBER)
        ),
        activeAppointmentCount = 1,
        showWithdrawalDialog = showWithdrawalDialog,
        onBackClick = onBackClick,
        onWithdrawClick = { showWithdrawalDialog = true },
        onWithdrawalDialogDismiss = { showWithdrawalDialog = false },
        onWithdrawalDialogConfirm = {
            showWithdrawalDialog = false
            onWithdrawComplete()
        },
        modifier = modifier
    )
}
