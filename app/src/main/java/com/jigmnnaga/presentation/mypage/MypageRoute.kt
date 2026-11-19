package com.jigmnnaga.presentation.mypage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    onDefaultLocationClick: () -> Unit = {},
    onLocationSharingSettingsClick: () -> Unit = {}
) {
    MypageScreen(
        state = MypageUiState.PreviewDefault,
        onBackClick = onBackClick,
        onEditProfileClick = onEditProfileClick,
        onNotificationSettingsClick = { /* TODO: 알림 설정 화면 연결 */ },
        onDefaultLocationClick = onDefaultLocationClick,
        onLocationSharingSettingsClick = onLocationSharingSettingsClick,
        onHelpClick = { /* TODO: 도움말 및 문의 화면 연결 */ },
        onServiceInfoClick = { /* TODO: 서비스 정보 화면 연결 */ },
        modifier = modifier
    )
}
