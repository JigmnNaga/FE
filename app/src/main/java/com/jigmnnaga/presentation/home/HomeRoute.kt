package com.jigmnnaga.presentation.home

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
import com.jigmnnaga.di.HomeEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.serialization.Serializable

/** 홈 화면 네비게이션 목적지 타입. */
@Serializable
data object HomeRoute

/**
 * 홈 화면 Route Composable.
 *
 * [HomeViewModel]을 생성하여 실제 API 데이터를 [HomeScreen]에 공급한다.
 *
 * - 탭 전환: [HomeViewModel.selectTab] — API 재호출 없이 이미 불러온 데이터를 재사용한다.
 * - 인증 오류: [HomeUiState.loginRequired]가 true 가 되면 [onLoginRequired]를 호출한다.
 *   HomeRoute가 Back Stack에서 제거되므로 로그인 후 새 HomeRoute 인스턴스가 생성된다.
 */
@Composable
fun HomeRoute(
    onAddClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAppointmentClick: (String) -> Unit,
    onInvitationClick: () -> Unit,
    onLoginRequired: () -> Unit,
    modifier: Modifier = Modifier
) {
    val applicationContext = LocalContext.current.applicationContext
    val viewModel: HomeViewModel = viewModel(
        factory = remember(applicationContext) {
            viewModelFactory {
                initializer {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        applicationContext,
                        HomeEntryPoint::class.java
                    )
                    HomeViewModel(entryPoint.homeRepository())
                }
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 인증 오류 감지 — loginRequired 가 true 가 되면 로그인 화면으로 이동한다.
    // onLoginRequired() 는 HomeRoute 를 Back Stack 에서 제거하므로 별도 소비(consume)가 필요 없다.
    LaunchedEffect(uiState.loginRequired) {
        if (uiState.loginRequired) {
            onLoginRequired()
        }
    }

    HomeScreen(
        state = uiState,
        onTabSelect = viewModel::selectTab,
        onAddClick = onAddClick,
        onScheduleClick = onScheduleClick,
        onNotificationClick = onNotificationClick,
        onProfileClick = onProfileClick,
        onAppointmentClick = onAppointmentClick,
        onInvitationClick = onInvitationClick,
        onRetry = viewModel::refresh,
        modifier = modifier
    )
}
