package com.jigmnnaga.presentation.appointment.route

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jigmnnaga.R

/**
 * 이동 경로 화면([RouteScreen])과 [RouteViewModel]을 연결한다.
 * 뒤로 가기는 경로 선택 단계에서 초기화면으로 먼저 돌아가고, 그 밖의 단계에서는 [onBackClick]으로 화면을 닫는다.
 * 지도 SDK는 아직 이 화면에 붙지 않아서 "내 위치로 지도 이동"은 아무 일도 하지 않는다.
 */
@Composable
fun RouteRoute(
    viewModel: RouteViewModel,
    onBackClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onPlaceClick: () -> Unit,
    onMoreClick: () -> Unit,
    onChatTabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val handleBack: () -> Unit = { if (!viewModel.onBack()) onBackClick() }

    BackHandler(onBack = handleBack)

    RouteScreen(
        state = state,
        onBackClick = handleBack,
        onCalendarClick = onCalendarClick,
        onPlaceClick = onPlaceClick,
        onMoreClick = onMoreClick,
        onChatTabClick = onChatTabClick,
        onMyCardClick = viewModel::onMyCardClick,
        onSelectTransport = viewModel::onSelectTransport,
        onToggleOption = viewModel::onToggleOption,
        onStartMoving = viewModel::onStartMoving,
        onToggleMyRoute = viewModel::onToggleMyRoute,
        onArrive = viewModel::onArrive,
        onMyLocationClick = {},
        modifier = modifier
    )
}

/** 멤버 카드 문구를 문자열 리소스에서 가져온다. */
@Composable
fun rememberRouteMemberLabels(): RouteMemberLabels {
    val notStarted = stringResource(R.string.route_member_not_started)
    val waiting = stringResource(R.string.route_member_waiting)
    val setRoute = stringResource(R.string.route_member_set_route_hint)
    return remember(notStarted, waiting, setRoute) { RouteMemberLabels(notStarted, waiting, setRoute) }
}
