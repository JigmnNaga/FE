package com.jigmnnaga.presentation.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jigmnnaga.core.navigation.AppNavigationGraph
import com.jigmnnaga.core.navigation.AppNavigator
import com.jigmnnaga.presentation.appointment.AppointmentCreateDestination

/**
 * 홈 기능이 소유하는 네비게이션 그래프.
 * MainActivity의 AppNavHost에 등록한다.
 *
 * 주의: 로그인 성공 후 홈으로 이동하는 연결은 별도 작업(백엔드 API 연동)에서 추가한다.
 */
object HomeNavigationGraph : AppNavigationGraph {
    override fun register(builder: NavGraphBuilder, navigator: AppNavigator) {
        builder.composable<HomeRoute> {
            HomeRoute(
                onAddClick = { navigator.navigate(AppointmentCreateDestination) },
                onScheduleClick = { /* TODO: CAL-001 캘린더 화면 연결 */ },
                onNotificationClick = { /* TODO: HOM-003 알림 화면 연결 */ },
                onProfileClick = { /* TODO: 마이페이지 화면 연결 */ },
                onAppointmentClick = { /* TODO: 약속 상세 화면 연결 */ },
                onInvitationClick = { /* TODO: 초대 목록 화면 연결 */ }
            )
        }
    }
}
