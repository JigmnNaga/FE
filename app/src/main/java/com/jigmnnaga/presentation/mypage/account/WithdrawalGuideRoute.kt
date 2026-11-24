package com.jigmnnaga.presentation.mypage.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.Serializable

/** 방장 회원탈퇴 안내 화면 네비게이션 목적지 타입. */
@Serializable
data object WithdrawalGuideRoute

/**
 * 방장 회원탈퇴 안내 화면 Route Composable.
 * 화면 진입 시 다가오는 약속 목록을 조회하고, [AccountViewModel]을 통해 회원탈퇴 API를 호출한다.
 */
@Composable
fun WithdrawalGuideRoute(
    viewModel: AccountViewModel,
    onBackClick: () -> Unit,
    onWithdrawComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showWithdrawalDialog by remember { mutableStateOf(false) }

    // 화면 진입 시 다가오는 약속 목록 조회
    LaunchedEffect(Unit) {
        viewModel.loadUpcomingAppointments()
    }

    // 회원탈퇴 성공 또는 이미 로그아웃 상태 → 완료 화면으로 이동
    LaunchedEffect(state.withdrawSuccess, state.loginRequired) {
        if (state.withdrawSuccess || state.loginRequired) {
            onWithdrawComplete()
        }
    }

    WithdrawalGuideScreen(
        rooms = state.rooms,
        activeAppointmentCount = state.rooms.size,
        showWithdrawalDialog = showWithdrawalDialog,
        onBackClick = onBackClick,
        onWithdrawClick = { showWithdrawalDialog = true },
        onWithdrawalDialogDismiss = { showWithdrawalDialog = false },
        onWithdrawalDialogConfirm = {
            showWithdrawalDialog = false
            viewModel.withdraw()
        },
        modifier = modifier
    )
}
