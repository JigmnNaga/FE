package com.jigmnnaga.presentation.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * 알림 화면 진입점. 화면에 들어오면 목록을 불러오고, ViewModel이 보내는 이벤트를 호스트에 넘긴다.
 * 로그인이 필요하면 [onLoginRequired]에 로그인 뒤 다시 불러오는 동작을 넘긴다.
 */
@Composable
fun NotificationRoute(
    viewModel: NotificationViewModel,
    onBack: () -> Unit,
    onOpenAppointment: (appointmentId: Long) -> Unit,
    onLoginRequired: (resumeAfterLogin: () -> Unit) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val event by viewModel.event.collectAsState()

    LaunchedEffect(viewModel) { viewModel.load() }

    LaunchedEffect(event) {
        when (val current = event) {
            NotificationEvent.LoginRequired -> onLoginRequired(viewModel::load)
            is NotificationEvent.OpenAppointment -> onOpenAppointment(current.appointmentId)
            null -> return@LaunchedEffect
        }
        viewModel.consumeEvent()
    }

    NotificationScreen(
        state = state,
        onBackClick = onBack,
        onTabSelect = viewModel::onTabSelect,
        onInviteConfirmClick = viewModel::onNotificationClick,
        onActivityClick = viewModel::onNotificationClick,
        onLoadMore = viewModel::loadMore,
        onRetryClick = viewModel::load
    )
}
