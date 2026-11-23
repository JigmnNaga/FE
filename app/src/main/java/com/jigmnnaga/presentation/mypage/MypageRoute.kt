package com.jigmnnaga.presentation.mypage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jigmnnaga.presentation.mypage.account.WithdrawalConfirmDialog
import kotlinx.serialization.Serializable

/** 마이페이지 메인 화면 네비게이션 목적지 타입. */
@Serializable
data object MypageRoute

/**
 * 마이페이지 화면 Route Composable.
 *
 * [viewModel]을 주입받아 프로필 및 약속 통계 API 데이터를 화면에 공급한다.
 * [onLoginRequired] 콜백이 호출되면 로그인 화면으로 이동해야 한다.
 */
@Composable
fun MypageRoute(
    viewModel: MypageViewModel,
    onBackClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onLoginRequired: () -> Unit,
    modifier: Modifier = Modifier,
    onNotificationSettingsClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onDefaultLocationClick: () -> Unit = {},
    onLocationSharingSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onWithdrawConfirmed: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.loginRequired) {
        if (state.loginRequired) {
            onLoginRequired()
        }
    }

    var showWithdrawalDialog by remember { mutableStateOf(false) }

    if (showWithdrawalDialog) {
        WithdrawalConfirmDialog(
            activeAppointmentCount = 2, // TODO: 활성 약속 수 API 연동 시 실제 값으로 교체
            onDismiss = { showWithdrawalDialog = false },
            onConfirm = {
                showWithdrawalDialog = false
                onWithdrawConfirmed()
            }
        )
    }

    MypageScreen(
        state = state,
        onBackClick = onBackClick,
        onEditProfileClick = onEditProfileClick,
        onNotificationSettingsClick = onNotificationSettingsClick,
        onHelpClick = onHelpClick,
        onDefaultLocationClick = onDefaultLocationClick,
        onLocationSharingSettingsClick = onLocationSharingSettingsClick,
        onServiceInfoClick = { /* TODO: 서비스 정보 화면 연결 */ },
        onLogoutClick = onLogoutClick,
        onWithdrawClick = { showWithdrawalDialog = true },
        modifier = modifier
    )
}
