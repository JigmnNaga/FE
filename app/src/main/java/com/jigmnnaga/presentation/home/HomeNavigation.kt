package com.jigmnnaga.presentation.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jigmnnaga.core.navigation.AppNavigationGraph
import com.jigmnnaga.core.navigation.AppNavigator
import com.jigmnnaga.presentation.appointment.AppointmentCreateDestination
import com.jigmnnaga.presentation.appointment.toAppointmentRoomDestination
import com.jigmnnaga.presentation.calendar.CalendarRoute
import com.jigmnnaga.presentation.notification.NotificationDestination

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
                onScheduleClick = { navigator.navigateSingleTop(CalendarRoute) },
                onNotificationClick = { navigator.navigate(NotificationDestination) },
                onProfileClick = { /* TODO: 마이페이지 화면 연결 */ },
                // 홈 약속 목록이 목업인 동안은 숫자가 아닌 ID를 무시한다. 홈 API 연동 뒤에는 서버 약속 ID가 들어온다.
                onAppointmentClick = { id ->
                    id.toAppointmentRoomDestination()?.let { navigator.navigate(it) }
                },
                onInvitationClick = { /* TODO: 초대 목록 화면 연결 */ }
            )
        }
    }
}
