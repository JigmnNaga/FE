package com.jigmnnaga.presentation.calendar

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jigmnnaga.core.navigation.AppNavigationGraph
import com.jigmnnaga.core.navigation.AppNavigationOptions
import com.jigmnnaga.core.navigation.AppNavigator
import com.jigmnnaga.presentation.appointment.AppointmentCreateDestination
import com.jigmnnaga.presentation.appointment.toAppointmentRoomDestination
import com.jigmnnaga.presentation.common.login.LoginRoute
import com.jigmnnaga.presentation.notification.NotificationDestination

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
                onNotificationClick = { navigator.navigate(NotificationDestination) },
                onProfileClick = { /* TODO: 마이페이지 화면 연결 */ },
                onAppointmentClick = { id ->
                    // toAppointmentRoomDestination()은 internal이나 같은 모듈이므로 접근 가능.
                    // 숫자가 아닌 ID면 null을 반환하므로 안전하게 무시한다.
                    id.toAppointmentRoomDestination()?.let { navigator.navigate(it) }
                },
                onLoginRequired = {
                    // CalendarRoute 를 Back Stack 에서 제거하고 로그인 화면으로 이동한다.
                    // 로그인 성공 후 MainActivity 의 기본 흐름에 따라 새 CalendarRoute 로 이동한다.
                    navigator.navigate(
                        destination = LoginRoute,
                        options = AppNavigationOptions(
                            popUpTo = CalendarRoute,
                            inclusive = true,
                            launchSingleTop = true
                        )
                    )
                }
            )
        }
    }
}
