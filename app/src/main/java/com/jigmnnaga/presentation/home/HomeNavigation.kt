package com.jigmnnaga.presentation.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jigmnnaga.core.navigation.AppNavigationGraph
import com.jigmnnaga.core.navigation.AppNavigationOptions
import com.jigmnnaga.core.navigation.AppNavigator
import com.jigmnnaga.presentation.appointment.AppointmentCreateDestination
import com.jigmnnaga.presentation.appointment.toAppointmentRoomDestination
import com.jigmnnaga.presentation.calendar.CalendarRoute
import com.jigmnnaga.presentation.common.login.LoginRoute
import com.jigmnnaga.presentation.mypage.MypageRoute
import com.jigmnnaga.presentation.notification.NotificationDestination

/**
 * 홈 기능이 소유하는 네비게이션 그래프.
 * MainActivity의 AppNavHost에 등록한다.
 */
object HomeNavigationGraph : AppNavigationGraph {
    override fun register(builder: NavGraphBuilder, navigator: AppNavigator) {
        builder.composable<HomeRoute> {
            HomeRoute(
                onAddClick = { navigator.navigate(AppointmentCreateDestination) },
                onScheduleClick = { navigator.navigateSingleTop(CalendarRoute) },
                onNotificationClick = { navigator.navigate(NotificationDestination) },
                onProfileClick = { navigator.navigate(MypageRoute) },
                onAppointmentClick = { id ->
                    id.toAppointmentRoomDestination()?.let { navigator.navigate(it) }
                },
                onInvitationClick = { /* TODO: 초대 목록 화면 연결 */ },
                onLoginRequired = {
                    // HomeRoute 를 Back Stack 에서 제거하고 로그인 화면으로 이동한다.
                    // 로그인 성공 후 MainActivity 의 기본 흐름에 따라 새 HomeRoute 로 이동한다.
                    navigator.navigate(
                        destination = LoginRoute,
                        options = AppNavigationOptions(
                            popUpTo = HomeRoute,
                            inclusive = true,
                            launchSingleTop = true
                        )
                    )
                }
            )
        }
    }
}
