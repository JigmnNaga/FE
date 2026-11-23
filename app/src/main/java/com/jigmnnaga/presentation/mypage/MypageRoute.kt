package com.jigmnnaga.presentation.mypage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jigmnnaga.presentation.mypage.account.WithdrawalConfirmDialog
import kotlinx.serialization.Serializable

/** 마이페이지 메인 화면 네비게이션 목적지 타입. */
@Serializable
data object MypageRoute

/**
 * 마이페이지 화면 Route Composable.
 *
 * ViewModel 연동 전까지는 [MypageUiState.PreviewDefault] Mock 데이터를 사용한다.
 * API 연동 시 MypageViewModel을 생성해 실제 데이터를 공급하면 된다.
 */
@Composable
fun MypageRoute(
    onBackClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNotificationSettingsClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onDefaultLocationClick: () -> Unit = {},
    onLocationSharingSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onWithdrawConfirmed: () -> Unit = {}
) {
    var showWithdrawalDialog by remember { mutableStateOf(false) }

    if (showWithdrawalDialog) {
        WithdrawalConfirmDialog(
            activeAppointmentCount = 2, // TODO: ViewModel 연동 시 실제 값으로 교체
            onDismiss = { showWithdrawalDialog = false },
            onConfirm = {
                showWithdrawalDialog = false
                onWithdrawConfirmed()
            }
        )
    }

    MypageScreen(
        state = MypageUiState.PreviewDefault,
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
