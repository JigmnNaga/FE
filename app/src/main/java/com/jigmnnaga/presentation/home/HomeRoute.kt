package com.jigmnnaga.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable

/** 홈 화면 네비게이션 목적지 타입. */
@Serializable
data object HomeRoute

/**
 * 홈 화면 Route Composable.
 *
 * API 연동 전까지는 목업 데이터를 사용하며, 탭 전환 상태를 직접 관리한다.
 * 추후 HomeViewModel을 주입하여 실제 데이터와 API 연동을 처리하면 된다.
 *
 * API 연동 시 교체 방법:
 * 1. HomeViewModel 생성 후 `viewModel: HomeViewModel = viewModel()` 파라미터 추가
 * 2. `val uiState by viewModel.uiState.collectAsStateWithLifecycle()` 로 상태 수집
 * 3. `remember { mutableStateOf(...) }` 블록 제거
 * 4. `onTabSelect = viewModel::selectTab` 으로 연결
 */
@Composable
fun HomeRoute(
    onAddClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAppointmentClick: (String) -> Unit,
    onInvitationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var uiState by remember { mutableStateOf(HomeUiState.PreviewWithList) }

    HomeScreen(
        state = uiState,
        onTabSelect = { tab -> uiState = uiState.copy(selectedTab = tab) },
        onAddClick = onAddClick,
        onScheduleClick = onScheduleClick,
        onNotificationClick = onNotificationClick,
        onProfileClick = onProfileClick,
        onAppointmentClick = onAppointmentClick,
        onInvitationClick = onInvitationClick,
        modifier = modifier
    )
}
