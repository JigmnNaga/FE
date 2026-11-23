package com.jigmnnaga.presentation.mypage.notification

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.jigmnnaga.core.navigation.AppNavigationGraph
import com.jigmnnaga.core.navigation.AppNavigator

/**
 * 알림 설정 화면의 네비게이션 그래프.
 * MainActivity의 AppNavHost에 등록하고, 마이페이지의 "알림 설정" 항목이 이 목적지로 이동한다.
 */
object NotificationSettingsNavigationGraph : AppNavigationGraph {
    override fun register(builder: NavGraphBuilder, navigator: AppNavigator) {
        builder.composable<NotificationSettingsRoute> {
            NotificationSettingsRoute(
                onBackClick = { navigator.navigateBack() }
            )
        }
    }
}
