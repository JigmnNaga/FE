package com.jigmnnaga.presentation.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jigmnnaga.di.CalendarEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.serialization.Serializable

/** 캘린더 화면 네비게이션 목적지 타입. */
@Serializable
data object CalendarRoute

/**
 * 캘린더 화면 Route Composable.
 *
 * [CalendarViewModel]을 생성하여 실제 API 데이터를 [CalendarScreen]에 공급한다.
 *
 * - 월 이동: [CalendarViewModel.goToPrevMonth] / [CalendarViewModel.goToNextMonth]
 * - 날짜 선택: [CalendarViewModel.selectDate] (같은 날짜 재선택 시 해제)
 * - 약속 카드 클릭: [onAppointmentClick] → Navigation으로 약속방 이동
 * - 재시도: [CalendarViewModel.retry]
 * - 인증 오류: [CalendarUiState.loginRequired]가 true 가 되면 [onLoginRequired]를 호출한다.
 *   CalendarRoute 를 Back Stack 에서 제거하므로 별도 소비(consume)가 필요 없다.
 */
@Composable
fun CalendarRoute(
    onAddClick: () -> Unit,
    onHomeClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAppointmentClick: (String) -> Unit,
    onLoginRequired: () -> Unit,
    modifier: Modifier = Modifier
) {
    val applicationContext = LocalContext.current.applicationContext
    val viewModel: CalendarViewModel = viewModel(
        factory = remember(applicationContext) {
            viewModelFactory {
                initializer {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        applicationContext,
                        CalendarEntryPoint::class.java
                    )
                    CalendarViewModel(entryPoint.calendarRepository())
                }
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 인증 오류 감지 — loginRequired 가 true 가 되면 로그인 화면으로 이동한다.
    LaunchedEffect(uiState.loginRequired) {
        if (uiState.loginRequired) {
            onLoginRequired()
        }
    }

    CalendarScreen(
        state = uiState,
        onPrevMonth = viewModel::goToPrevMonth,
        onNextMonth = viewModel::goToNextMonth,
        onDateSelect = viewModel::selectDate,
        onAppointmentClick = onAppointmentClick,
        onRetry = viewModel::retry,
        onAddClick = onAddClick,
        onHomeClick = onHomeClick,
        onNotificationClick = onNotificationClick,
        onProfileClick = onProfileClick,
        modifier = modifier
    )
}
