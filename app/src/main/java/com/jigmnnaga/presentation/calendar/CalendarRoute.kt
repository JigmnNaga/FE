package com.jigmnnaga.presentation.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable

/** 캘린더 화면 네비게이션 목적지 타입. */
@Serializable
data object CalendarRoute

/**
 * 캘린더 화면 Route Composable.
 *
 * API 연동 전까지는 목업 데이터를 사용하며, 월 이동 상태를 직접 관리한다.
 * 추후 CalendarViewModel을 주입하여 실제 데이터와 API 연동을 처리하면 된다.
 *
 * API 연동 시 교체 방법:
 * 1. CalendarViewModel 생성 후 `viewModel: CalendarViewModel = viewModel()` 파라미터 추가
 * 2. `val uiState by viewModel.uiState.collectAsStateWithLifecycle()` 로 상태 수집
 * 3. `remember { mutableStateOf(...) }` 블록 제거
 * 4. `onPrevMonth = viewModel::onPrevMonth` 등으로 연결
 */
@Composable
fun CalendarRoute(
    onAddClick: () -> Unit,
    onHomeClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var uiState by remember { mutableStateOf(CalendarUiState.initial()) }

    CalendarScreen(
        state = uiState,
        onPrevMonth = {
            uiState = uiState.copy(displayMonth = uiState.displayMonth.prev())
        },
        onNextMonth = {
            uiState = uiState.copy(displayMonth = uiState.displayMonth.next())
        },
        onAddClick = onAddClick,
        onHomeClick = onHomeClick,
        onNotificationClick = onNotificationClick,
        onProfileClick = onProfileClick,
        modifier = modifier
    )
}
