package com.jigmnnaga.presentation.appointment.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.jigmnnaga.core.designsystem.theme.AppColors

/**
 * 희망 날짜·시간 선택 화면과 ViewModel을 연결한다.
 * 약속 제목과 참여자 목록이 필요하므로 ViewModel은 호출하는 쪽에서 만들어 넘긴다.
 * 시스템 바와 키보드 inset은 여기서 처리한다.
 */
@Composable
fun AppointmentDateTimeRoute(
    viewModel: AppointmentDateTimeViewModel,
    onExit: () -> Unit,
    onGoToChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.MainGreen.Green200)
            .safeDrawingPadding()
    ) {
        AppointmentDateTimeScreen(
            state = state,
            onBack = { if (!viewModel.movePrevious()) onExit() },
            onStatusToggle = viewModel::onStatusToggle,
            onMonthChange = viewModel::onMonthChange,
            onDateToggle = viewModel::onDateToggle,
            onActiveDateSelect = viewModel::onActiveDateSelect,
            onStartTimeClick = viewModel::onStartTimeClick,
            onEndTimeClick = viewModel::onEndTimeClick,
            onQuickTimeClick = viewModel::onQuickTimeClick,
            onTimePickerValueChange = viewModel::onTimePickerValueChange,
            onTimePickerDismiss = viewModel::onTimePickerDismiss,
            onTimePickerConfirm = viewModel::onTimePickerConfirm,
            onNext = viewModel::onNext,
            onGoToChat = onGoToChat
        )
    }
}