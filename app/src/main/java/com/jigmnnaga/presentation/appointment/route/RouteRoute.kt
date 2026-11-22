package com.jigmnnaga.presentation.appointment.route

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jigmnnaga.R
import com.jigmnnaga.presentation.common.error.ErrorScreen
import com.jigmnnaga.presentation.common.error.ErrorType

/**
 * 약속방 "지도·현황" 탭에 들어가는 이동 경로 내용([RouteBody])과 [RouteViewModel]을 연결한다.
 * 헤더와 "채팅 / 지도·현황" 탭은 약속방이 그리므로 여기서는 본문만 그린다.
 * 뒤로 가기는 경로 선택 단계에서 초기화면으로만 돌아가고, 그 밖의 단계에서는 약속방의 기본 동작을 따른다.
 * 지도 SDK는 아직 이 화면에 붙지 않아서 "내 위치로 지도 이동"은 아무 일도 하지 않는다.
 */
@Composable
fun RouteRoute(
    viewModel: RouteViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    BackHandler(enabled = state.stage == RouteStage.SELECT) { viewModel.onBack() }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 앱 설정에서 권한을 바꾸고 돌아오면 화면이 다시 앞에 오는 때 권한 상태를 새로 확인한다.
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onLocationPermissionChecked(context.hasLocationPermission())
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) viewModel.requestCurrentLocation() else viewModel.onCurrentLocationPermissionDenied()
    }

    if (state.needsLocationPermission) {
        ErrorScreen(
            type = ErrorType.LocationPermission,
            onActionClick = { context.openAppSettings() },
            modifier = modifier
        )
    } else RouteBody(
        state = state,
        onMyCardClick = viewModel::onMyCardClick,
        onSelectTransport = viewModel::onSelectTransport,
        onToggleOption = viewModel::onToggleOption,
        onStartMoving = viewModel::onStartMoving,
        onToggleMyRoute = viewModel::onToggleMyRoute,
        onArrive = viewModel::onArrive,
        onMyLocationClick = {},
        modifier = modifier,
        onOpenStartPicker = viewModel::onOpenStartPicker,
        onRetryOptions = viewModel::onRetryTravelOptions
    )

    if (state.startPicker.isOpen) {
        // 약속방 헤더와 탭까지 덮는 전체 화면. 시스템 뒤로 가기는 Dialog가 받아 선택 화면만 닫는다.
        Dialog(
            onDismissRequest = viewModel::onCloseStartPicker,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            RouteStartPickerScreen(
                state = state.startPicker,
                onBackClick = viewModel::onCloseStartPicker,
                onSelect = viewModel::onSelectStartCandidate,
                onCurrentLocationClick = {
                    if (context.hasLocationPermission()) {
                        viewModel.onCurrentLocationClick()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                },
                onQueryChange = viewModel::onStartQueryChange,
                onConfirm = viewModel::onConfirmStartLocation
            )
        }
    }
}

/** 멤버 카드 문구를 문자열 리소스에서 가져온다. */
@Composable
fun rememberRouteMemberLabels(): RouteMemberLabels {
    val notStarted = stringResource(R.string.route_member_not_started)
    val waiting = stringResource(R.string.route_member_waiting)
    val setRoute = stringResource(R.string.route_member_set_route_hint)
    return remember(notStarted, waiting, setRoute) { RouteMemberLabels(notStarted, waiting, setRoute) }
}
