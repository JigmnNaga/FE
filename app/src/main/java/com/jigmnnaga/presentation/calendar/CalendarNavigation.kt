package com.jigmnnaga.presentation.calendar

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jigmnnaga.core.navigation.AppNavigationGraph
import com.jigmnnaga.core.navigation.AppNavigator
import com.jigmnnaga.presentation.appointment.AppointmentCreateDestination

/**
 * 캘린더 기능이 소유하는 네비게이션 그래프.
 * MainActivity의 AppNavHost에 등록한다.
 */
object CalendarNavigationGraph : AppNavigationGraph {
    override fun register(builder: NavGraphBuilder, navigator: AppNavigator) {
        builder.composable<CalendarRoute> {
            CalendarRoute(
                onAddClick = { navigator.navigate(AppointmentCreateDestination) },
                onHomeClick = { navigator.navigateBack() },
                onNotificationClick = { /* TODO: HOM-003 알림 화면 연결 */ },
                onProfileClick = { /* TODO: 마이페이지 화면 연결 */ }
            )
        }
    }
}
